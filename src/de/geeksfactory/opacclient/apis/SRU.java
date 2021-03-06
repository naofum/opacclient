package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import de.geeksfactory.opacclient.ISBNTools;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public class SRU extends BaseApi implements OpacApi {
	
	protected String opac_url = "";
	protected JSONObject data;
	protected MetaDataSource metadata;
	protected boolean initialised = false;
	protected Library library;
	protected int resultcount = 10;
	private String currentSearchParams;
	private Document searchDoc;
	private HashMap<String, String> searchQueries = new HashMap<String, String>();
	private String idSearchQuery;
	protected String shareUrl;
	
	protected static HashMap<String, MediaType> defaulttypes = new HashMap<String, MediaType>();
	static {
		defaulttypes.put("print", MediaType.BOOK);
		defaulttypes.put("large print", MediaType.BOOK);
		defaulttypes.put("braille", MediaType.UNKNOWN);
		defaulttypes.put("electronic", MediaType.EBOOK);
		defaulttypes.put("microfiche", MediaType.UNKNOWN);
		defaulttypes.put("microfilm", MediaType.UNKNOWN);
		defaulttypes.put("Tontraeger", MediaType.AUDIOBOOK);
	}
	
	@Override
	public void init(MetaDataSource metadata, Library lib) {
		super.init(metadata, lib);

		this.metadata = metadata;
		this.library = lib;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
			JSONObject searchQueriesJson = data.getJSONObject("searchqueries");
			addSearchQueries(searchQueriesJson);
			if(data.has("sharelink"))
				shareUrl = data.getString("sharelink");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	

	private void addSearchQueries(JSONObject searchQueriesJson) {
		String[] queries = {
				KEY_SEARCH_QUERY_FREE, KEY_SEARCH_QUERY_TITLE,
				KEY_SEARCH_QUERY_AUTHOR, KEY_SEARCH_QUERY_KEYWORDA,
				KEY_SEARCH_QUERY_KEYWORDB, KEY_SEARCH_QUERY_BRANCH,
				KEY_SEARCH_QUERY_HOME_BRANCH, KEY_SEARCH_QUERY_ISBN,
				KEY_SEARCH_QUERY_YEAR, KEY_SEARCH_QUERY_YEAR_RANGE_START,
				KEY_SEARCH_QUERY_YEAR_RANGE_END, KEY_SEARCH_QUERY_SYSTEM,
				KEY_SEARCH_QUERY_AUDIENCE, KEY_SEARCH_QUERY_PUBLISHER,
				KEY_SEARCH_QUERY_CATEGORY, KEY_SEARCH_QUERY_BARCODE,
				KEY_SEARCH_QUERY_LOCATION, KEY_SEARCH_QUERY_DIGITAL
		};
		for(String query:queries) {
			if(searchQueriesJson.has(query))
				try {
					searchQueries.put(query, searchQueriesJson.getString(query));
				} catch (JSONException e) {
					e.printStackTrace();
				}
		}
		try {
			if(searchQueriesJson.has("id")) {
				idSearchQuery = searchQueriesJson.getString("id");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start() throws IOException, NotReachableException {
		try {
			metadata.open();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (!metadata.hasMeta(library.getIdent())) {
			metadata.close();
			//extract_meta();
		} else {
			metadata.close();
		}
	}
	
	protected int addParameters(Map<String, String> query, String key, String searchkey,
			StringBuilder params, int index) {
		if (!query.containsKey(key) || query.get(key).equals(""))
			return index;
		if(index != 0) params.append("%20and%20");
		params.append(searchkey + "%3D" + query.get(key));
		return index + 1;

	}

	@Override
	public SearchRequestResult search(Map<String, String> query) throws IOException,
			NotReachableException, OpacErrorException {
		StringBuilder params = new StringBuilder();

		int index = 0;
		start();

		for(String parameter:searchQueries.keySet()) {
			index = addParameters(query, parameter,
					searchQueries.get(parameter), params, index);
		}
		
		if (index == 0) {
			throw new OpacErrorException(
					"Es wurden keine Suchkriterien eingegeben.");
		}
		currentSearchParams = params.toString();
		String xml = httpGet(opac_url +
				"?version=1.1&operation=searchRetrieve&maximumRecords=" + resultcount +
				"&recordSchema=mods&sortKeys=relevance,,1&query=" + currentSearchParams,
				getDefaultEncoding());
		
		return parse_result(xml);
	}

	private SearchRequestResult parse_result(String xml) throws OpacErrorException {
		searchDoc = Jsoup.parse(xml, "", Parser.xmlParser());
		if(searchDoc.select("diag|diagnostic").size() > 0) {
			throw new OpacErrorException(searchDoc.select("diag|message").text());
		}
		
		int resultcount = 0;
		List<SearchResult> results = new ArrayList<SearchResult>();
		
		resultcount = Integer.valueOf(searchDoc.select("zs|numberOfRecords").text());
		
		Elements records = searchDoc.select("zs|records > zs|record");
		int i = 0;
		for(Element record:records) {
			SearchResult sr = new SearchResult();
			String title = getDetail(record, "titleInfo title");
			String firstName = getDetail(record, "name > namePart[type=given]");
			String lastName = getDetail(record, "name > namePart[type=family]");
			String year = getDetail(record, "dateIssued");
			String mType = getDetail(record, "physicalDescription > form");
			String isbn =  getDetail(record, "identifier[type=isbn]");
			String coverUrl = getDetail(record, "url[displayLabel=C Cover]");
			String additionalInfo = firstName + " " + lastName + ", " + year;
			sr.setInnerhtml("<b>" + title + "</b><br>" + additionalInfo);
			sr.setType(defaulttypes.get(mType));
			sr.setNr(i);
			sr.setId(getDetail(record, "recordIdentifier"));
			if (coverUrl.equals(""))
				sr.setCover(ISBNTools.getAmazonCoverURL(isbn, false));
			else
				sr.setCover(coverUrl);
			results.add(sr);
			i++;
		}
		
		return new SearchRequestResult(results, resultcount, 1);
	}

	private String getDetail(Element record, String selector) {
		if(record.select(selector).size() > 0) {
			return record.select(selector).first().text();
		} else {
			return "";
		}
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException {
		return null;
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		if (!initialised)
			start();
		
		String xml = httpGet(opac_url +
				"?version=1.1&operation=searchRetrieve&maximumRecords=" + resultcount +
				"&recordSchema=mods&sortKeys=relevance,,1&startRecord=" +
				String.valueOf(page*resultcount + 1) + "&query=" + currentSearchParams,
				getDefaultEncoding());
		return parse_result(xml);
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException, OpacErrorException {	
		if(idSearchQuery != null) {
			String xml = httpGet(opac_url +
					"?version=1.1&operation=searchRetrieve&maximumRecords=" + resultcount +
					"&recordSchema=mods&sortKeys=relevance,,1&query=" + idSearchQuery + "%3D" + id,
					getDefaultEncoding());
			searchDoc = Jsoup.parse(xml, "", Parser.xmlParser());
			if(searchDoc.select("diag|diagnostic").size() > 0) {
				throw new OpacErrorException(searchDoc.select("diag|message").text());
			}
			if(searchDoc.select("zs|record").size() != 1) { //should not happen
				throw new OpacErrorException("nicht gefunden");
			}
			return parse_detail(searchDoc.select("zs|record").first());
		} else {
			return null;
		}
	}

	private DetailledItem parse_detail(Element record) {
		String title = getDetail(record, "titleInfo title");
		String firstName = getDetail(record, "name > namePart[type=given]");
		String lastName = getDetail(record, "name > namePart[type=family]");
		String year = getDetail(record, "dateIssued");
		String desc = getDetail(record, "abstract");
		String isbn = getDetail(record, "identifier[type=isbn]");
		String coverUrl = getDetail(record, "url[displayLabel=C Cover]");
		
		DetailledItem item = new DetailledItem();
		item.setTitle(title);
		item.addDetail(new Detail("Autor", firstName + " " + lastName));
		item.addDetail(new Detail("Jahr", year));
		item.addDetail(new Detail("Beschreibung", desc));
		if (coverUrl.equals("") && isbn.length() > 0) {
			item.setCover(ISBNTools.getAmazonCoverURL(isbn, true));
		} else if (!coverUrl.equals("")) {
			item.setCover(coverUrl);
		}
		
		if(isbn.length() > 0) {
			item.addDetail(new Detail("ISBN", isbn));
		}
		
		return item;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException,
			OpacErrorException {
		return parse_detail(searchDoc.select("zs|records > zs|record").get(position));
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		return null;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		return null;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		return null;
	}

	@Override
	public String[] getSearchFields() {
		return searchQueries.keySet().toArray(new String[0]);
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return false;
	}

	@Override
	public boolean isAccountExtendable() {
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		if(shareUrl != null)
			return String.format(shareUrl, id);
		else
			return null;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ENDLESS_SCROLLING;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		return null;
	}
	
	@Override
	protected String getDefaultEncoding() {
		return "UTF-8";
	}

}
