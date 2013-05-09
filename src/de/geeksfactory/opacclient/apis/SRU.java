package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Bundle;
import android.util.Xml;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.networking.HTTPClient;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * OpacApi implementation for the open protocol "Search/Retrieve via URL",
 * developed by the library of congress
 */
public class SRU implements OpacApi {

	protected static HashMap<String, MediaType> typeofresourceToType = new HashMap<String, MediaType>();

	static {
		typeofresourceToType.put("text", MediaType.BOOK);
		typeofresourceToType.put("cartographic", MediaType.MAP);
		typeofresourceToType.put("notated music", MediaType.SCORE_MUSIC);
		typeofresourceToType.put("sound recording-musical", MediaType.CD_MUSIC);
		typeofresourceToType.put("sound recording-nonmusical", MediaType.CD);
		typeofresourceToType.put("sound recording", MediaType.CD);
		typeofresourceToType.put("still image", MediaType.ART);
		typeofresourceToType.put("moving image", MediaType.MOVIE);
		typeofresourceToType.put("three dimensional object", MediaType.UNKNOWN);
		typeofresourceToType.put("software, multimedia", MediaType.CD_SOFTWARE);
		typeofresourceToType.put("mixed material", MediaType.PACKAGE);
	}

	// Important URLs
	// http://sru.gbv.de/opac-de-wim2?version=1.1&operation=searchRetrieve&query=pica.tit%3Dentwicklung&maximumRecords=10&recordSchema=mods
	// http://www.loc.gov/standards/mods/mods-outline.html#titleInfo

	protected String opac_url = "";
	protected JSONObject data;
	protected DefaultHttpClient ahc;
	protected MetaDataSource metadata;
	protected String last_error = "";
	protected Bundle last_query;

	protected static final int FETCH_LENGTH = 20;

	protected static HashMap<String, Integer> nameparts_order = new HashMap<String, Integer>();

	protected String httpGet(String url) throws ClientProtocolException,
			IOException {
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = ahc.execute(httpget);
		if (response.getStatusLine().getStatusCode() >= 400) {
			throw new NotReachableException();
		}
		String html = convertStreamToString(response.getEntity().getContent());
		response.getEntity().consumeContent();
		return html;
	}

	@Override
	public String[] getSearchFields() {
		return new String[] { KEY_SEARCH_QUERY_TITLE };
	}

	@Override
	public String getLast_error() {
		return last_error;
	}

	protected String convertStreamToString(InputStream is) throws IOException {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
		} catch (UnsupportedEncodingException e1) {
			reader = new BufferedReader(new InputStreamReader(is));
		}
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append((line + "\n"));
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public void extract_meta(String html) {
		// Zweigstellen und Mediengruppen auslesen

		metadata.open();

		metadata.close();
	}

	@Override
	public void start() throws ClientProtocolException, SocketException,
			IOException, NotReachableException {

		// metadata.open();
		// if (!metadata.hasMeta(library.getIdent())) {
		// HttpPost httppost = new HttpPost();
		// List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		// nameValuePairs.add(new BasicNameValuePair("link_profis.x", "0"));
		// nameValuePairs.add(new BasicNameValuePair("link_profis.y", "1"));
		// httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		// String html = httpPost(opac_url + "/index.asp",
		// new UrlEncodedFormEntity(nameValuePairs));
		// metadata.close();
		// extract_meta(html);
		// } else {
		// metadata.close();
		// }
	}

	@Override
	public void init(MetaDataSource metadata, Library lib) {
		ahc = HTTPClient.getNewHttpClient(lib);

		this.metadata = metadata;
		this.data = lib.getData();

		try {
			this.opac_url = data.getString("baseurl");
		} catch (JSONException e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}

	public static String getStringFromBundle(Bundle bundle, String key) {
		// Workaround for Bundle.getString(key, default) being available not
		// before API 12
		String res = bundle.getString(key);
		if (res == null)
			res = "";
		else
			res = res.trim();
		return res;
	}

	@Override
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException {
		String url = opac_url
				+ "?version=1.1&operation=searchRetrieve&query=pica.tit%3D"
				+ query.getString(OpacApi.KEY_SEARCH_QUERY_TITLE)
				+ "&maximumRecords=" + FETCH_LENGTH + "&recordSchema=mods";

		HttpGet httpget = new HttpGet(url);
		HttpResponse response = ahc.execute(httpget);
		InputStream xmlStream = null;

		try {
			if (response.getStatusLine().getStatusCode() >= 400) {
				throw new NotReachableException();
			}
			xmlStream = response.getEntity().getContent();

			SearchXmlParser parser = new SearchXmlParser();
			return parser.parse(xmlStream, 1);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			if (xmlStream != null)
				xmlStream.close();
			response.getEntity().consumeContent();
		}
	}

	private class SearchXmlParser {
		protected static final String XML_NAMESPACE_SRW = "http://www.loc.gov/zing/srw/";
		protected static final String XML_NAMESPACE_MODS = "http://www.loc.gov/mods/v3";

		private int page_index;

		public SearchRequestResult parse(InputStream xml, int page_index)
				throws XmlPullParserException, IOException {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
			parser.setInput(xml, "UTF-8");
			parser.nextTag();
			this.page_index = page_index;
			return readSearchRetrieveResponse(parser);
		}

		private SearchRequestResult readSearchRetrieveResponse(
				XmlPullParser parser) throws IOException,
				XmlPullParserException {
			List<SearchResult> results = new ArrayList<SearchResult>();
			int total_result_count = -1;

			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_SRW,
					"searchRetrieveResponse");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				String nameSpace = parser.getNamespace();

				if (nameSpace.equals(XML_NAMESPACE_SRW)) {
					if (name.equals("numberOfRecords")) {
						total_result_count = parseNumberOfRecords(parser);
					} else if (name.equals("records")) {
						results.addAll(parseRecords(parser));
					} else {
						skip(parser);
					}
				} else {
					skip(parser);
				}
			}

			return new SearchRequestResult(results, total_result_count,
					(int) Math.ceil((float) total_result_count / FETCH_LENGTH),
					page_index);
		}

		private List<SearchResult> parseRecords(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			List<SearchResult> records = new ArrayList<SearchResult>();
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_SRW,
					"records");
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				String nameSpace = parser.getNamespace();

				if (nameSpace.equals(XML_NAMESPACE_SRW)) {
					if (name.equals("record")) {
						records.add(parseRecord(parser));
					} else {
						skip(parser);
					}
				} else {
					skip(parser);
				}
			}
			return records;
		}

		private int parseNumberOfRecords(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_SRW,
					"numberOfRecords");
			String title = readText(parser);
			parser.require(XmlPullParser.END_TAG, XML_NAMESPACE_SRW,
					"numberOfRecords");
			return Integer.parseInt(title);
		}

		private SearchResult parseRecord(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_SRW, "record");
			SearchResult entry = null;
			String schema = null;
			String packing = null;
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals("recordSchema")) {
					schema = readText(parser);
				} else if (name.equals("recordPacking")) {
					packing = readText(parser);
				} else if (name.equals("recordData")) {
					entry = readRecordData(parser, schema, packing);
				} else {
					skip(parser);
				}
			}
			return entry;
		}

		private SearchResult readRecordData(XmlPullParser parser,
				String schema, String packing) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_SRW,
					"recordData");
			if (schema.equals("mods") && packing.equals("xml")) {
				parser.next();
				SearchResult res = readMods(parser);
				parser.nextTag();
				return res;
			} else {
				throw new RuntimeException(
						"Record data format not supported / not recognized: "
								+ schema + "/" + packing);
			}
		}

		private SearchResult readMods(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_MODS, "mods");

			SearchResult result = new SearchResult();
			String title = "";
			List<String> names = new ArrayList<String>();
			List<String> namesPrimary = new ArrayList<String>();

			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (!parser.getNamespace().equals(XML_NAMESPACE_MODS)) {
					skip(parser);
				} else if (name.equals("titleInfo")) {
					title = readModsTitleInfo(parser);
				} else if (name.equals("typeOfResource")) {
					result.setType(readModsTypeOfResource(parser));
				} else if (name.equals("name")) {
					String usage = parser.getAttributeValue(null, "usage");
					String _name = readModsName(parser);
					names.add(_name);
					if ("primary".equals(usage)) {
						namesPrimary.add(_name);
					}
				} else {
					skip(parser);
				}
			}
			String author = "";
			if (namesPrimary.size() > 0) {
				author = StringUtils.join(namesPrimary, "; ");
			} else {
				author = StringUtils.join(
						names.subList(0, Math.min(names.size(), 4)), "; ");
			}
			result.setInnerhtml(title + "<br />" + author);
			return result;
		}

		private String readModsName(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_MODS, "name");

			String personname = "";

			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (!parser.getNamespace().equals(XML_NAMESPACE_MODS)) {
					skip(parser);
				} else if (name.equals("namePart")) {
					String type = parser.getAttributeValue(null, "type");
					String part = readText(parser);
					if ("family".equals(type)) {
						personname += part;
					} else if ("given".equals(type)) {
						personname = part + " " + personname;
					} else {
						personname += " " + part;
					}
				} else {
					skip(parser);
				}
			}
			return personname.trim();
		}

		private String readModsTitleInfo(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_MODS,
					"titleInfo");

			List<String> nameParts = new ArrayList<String>();

			String mainTitle = "";
			String subTitle = "";

			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (!parser.getNamespace().equals(XML_NAMESPACE_MODS)) {
					skip(parser);
				} else if (name.equals("nonSort")) {
					mainTitle += readText(parser);
				} else if (name.equals("title")) {
					mainTitle += readText(parser);
				} else if (name.equals("subTitle")) {
					subTitle += readText(parser);
				} else if (name.equals("partNumber")) {
					subTitle += readText(parser);
				} else if (name.equals("partName")) {
					subTitle += readText(parser);
				} else {
					skip(parser);
				}
			}
			if (subTitle.length() > 0) {
				return "<b>" + mainTitle + "</b><br />" + subTitle;
			} else {
				return "<b>" + mainTitle + "</b>";
			}
		}

		private MediaType readModsTypeOfResource(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			parser.require(XmlPullParser.START_TAG, XML_NAMESPACE_MODS,
					"typeOfResource");
			String content = readText(parser).trim();
			if (typeofresourceToType.containsKey(content)) {
				return typeofresourceToType.get(content);
			} else {
				return MediaType.NONE;
			}
		}

		private String readText(XmlPullParser parser) throws IOException,
				XmlPullParserException {
			String result = "";
			if (parser.next() == XmlPullParser.TEXT) {
				result = parser.getText();
				parser.nextTag();
			}
			return result.trim();
		}

		private String readTextIncludingChildren(XmlPullParser parser)
				throws IOException, XmlPullParserException {
			String result = "";
			String outerTagName = parser.getName();
			do {
				if (parser.next() == XmlPullParser.TEXT) {
					result += parser.getText();
				}
			} while (!(parser.next() == XmlPullParser.END_TAG && parser
					.getName().equals(outerTagName)));
			return result.trim();
		}

		private void skip(XmlPullParser parser) throws XmlPullParserException,
				IOException {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				throw new IllegalStateException();
			}
			int depth = 1;
			while (depth != 0) {
				switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
				}
			}
		}
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException {
		return null;
	}

	@Override
	public DetailledItem getResultById(String a, String homebranch)
			throws IOException, NotReachableException {
		return null;
	}

	@Override
	public DetailledItem getResult(int nr) throws IOException {
		return null;
	}

	@Override
	public ReservationResult reservation(String reservation_info, Account acc,
			int useraction, String selection) throws IOException {
		return null;
	}

	@Override
	public boolean prolong(Account account, String a) throws IOException,
			NotReachableException {
		return false;
	}

	@Override
	public boolean prolongAll(Account account) throws IOException {
		return false;
	}

	@Override
	public boolean cancel(Account account, String a) throws IOException,
			NotReachableException {
		return false;
	}

	@Override
	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, SocketException {
		return null;
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
	public String getAccountExtendableInfo(Account acc)
			throws ClientProtocolException, SocketException, IOException,
			NotReachableException {
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		return null;
	}

	@Override
	public int getSupportFlags() {
		return 0;
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option) {
		return null;
	}
}
