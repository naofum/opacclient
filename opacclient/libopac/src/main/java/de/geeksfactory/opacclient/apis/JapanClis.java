/**
 * Copyright (C) 2014 by Naofumi Fukue under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.apis;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Copy;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.objects.SearchResult.MediaType;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.utils.ISBNTools;
import okhttp3.FormBody;

/**
 * @author Johan von Forstner, 16.09.2013
 */

public class JapanClis extends ApacheBaseApi {

    protected static HashMap<String, MediaType> defaulttypes = new HashMap<>();
    protected static HashMap<String, String> languageCodes = new HashMap<>();

    static {
        defaulttypes.put("一般", MediaType.BOOK);
        defaulttypes.put("新書", MediaType.BOOK);
        defaulttypes.put("本", MediaType.BOOK);
        defaulttypes.put("図書", MediaType.BOOK);
        defaulttypes.put("図書資料", MediaType.BOOK);
        defaulttypes.put("図書その他", MediaType.BOOK);
        defaulttypes.put("文庫", MediaType.BOOK);
        defaulttypes.put("文庫本", MediaType.BOOK);
        defaulttypes.put("旅行案内", MediaType.BOOK);
        defaulttypes.put("参考資料", MediaType.BOOK);
        defaulttypes.put("例規集", MediaType.BOOK);
        defaulttypes.put("和綴資料", MediaType.BOOK);
        defaulttypes.put("大活字", MediaType.BOOK);
        defaulttypes.put("大活字本", MediaType.BOOK);
        defaulttypes.put("拡大写本", MediaType.BOOK);
        defaulttypes.put("外国語", MediaType.BOOK);
        defaulttypes.put("まんが", MediaType.BOOK);
        defaulttypes.put("点字資料", MediaType.BOOK);
        defaulttypes.put("点字付資料", MediaType.BOOK);
        defaulttypes.put("新聞", MediaType.NEWSPAPER);
        defaulttypes.put("新聞縮刷版", MediaType.NEWSPAPER);
        defaulttypes.put("雑誌", MediaType.MAGAZINE);
        defaulttypes.put("雑誌（点字）", MediaType.MAGAZINE);
        defaulttypes.put("絵本", MediaType.ART);
        defaulttypes.put("特大絵本", MediaType.ART);
        defaulttypes.put("しかけ絵本", MediaType.ART);
        defaulttypes.put("布の絵本", MediaType.ART);
        defaulttypes.put("紙芝居", MediaType.ART);
        defaulttypes.put("ポスター", MediaType.ART);
        defaulttypes.put("絵画", MediaType.ART);
        defaulttypes.put("写真", MediaType.ART);
        defaulttypes.put("パンフレット", MediaType.ART);
        defaulttypes.put("図譜・図案等", MediaType.ART);
        defaulttypes.put("地図", MediaType.MAP);
        defaulttypes.put("地図（１枚もの）", MediaType.MAP);
        defaulttypes.put("楽譜", MediaType.SCORE_MUSIC);
        defaulttypes.put("ＣＤ－ＲＯＭ", MediaType.CD_SOFTWARE);
        defaulttypes.put("マイクロフィルム", MediaType.UNKNOWN);
        defaulttypes.put("区分なし", MediaType.UNKNOWN);
        defaulttypes.put("その他作成物", MediaType.UNKNOWN);
        defaulttypes.put("ＣＤ", MediaType.CD);
        defaulttypes.put("朗読ＣＤ", MediaType.CD);
        defaulttypes.put("コンパクトディスク", MediaType.CD);
        defaulttypes.put("ＤＶＤ", MediaType.DVD);
        defaulttypes.put("ビデオテープ", MediaType.MOVIE);
        defaulttypes.put("ビデオテープ（ＶＨＳ）", MediaType.MOVIE);
        defaulttypes.put("カセットテープ", MediaType.AUDIO_CASSETTE);
        defaulttypes.put("カセット", MediaType.AUDIO_CASSETTE);

        languageCodes.put("ja", "JP");
        languageCodes.put("en", "EN");
    }

    protected String opac_url = "";
    protected String https_url = "";
    protected JSONObject data;
    protected Library library;
    protected int resultcount = 10;
    protected String reusehtml;
    protected Integer searchSet;
    protected String db;
    protected String pwEncoded;
    protected String languageCode;
    protected CookieStore cookieStore = new BasicCookieStore();
    protected String lor_reservations;

    protected String contact;
    protected String lcod;
    protected String num;
    protected String ctg;
    protected String shp;
    protected String rtn;
    protected String sid;
    protected String tm;
    protected String aut;

    @Override
    public void init(Library lib, HttpClientFactory httpClientFactory) {
        super.init(lib, httpClientFactory);

        this.library = lib;
        this.data = lib.getData();

        try {
            this.opac_url = data.getString("baseurl");
            this.db = data.getString("db");
            if (library.isAccountSupported()) {
                if (data.has("httpsbaseurl")) {
                    this.https_url = data.getString("httpsbaseurl");
                } else {
                    this.https_url = this.opac_url;
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected int addParameters(SearchQuery query, List<NameValuePair> params,
            int index) throws JSONException {
        if (query.getValue().equals("") || query.getValue().equals("false")) {
            return index;
        }
        if (query.getSearchField() instanceof TextSearchField
                || query.getSearchField() instanceof BarcodeSearchField) {
            if (query.getSearchField().getData().getBoolean("ADI")) {
                params.add(new BasicNameValuePair(query.getKey(), query
                        .getValue()));
            } else {
                params.add(new BasicNameValuePair("KEY" + index, query.getKey()));
                params.add(new BasicNameValuePair("ITEM" + index, query.getValue()));
                params.add(new BasicNameValuePair("COMP" + index, "3"));
                return index + 1;
            }
        } else if (query.getSearchField() instanceof CheckboxSearchField) {
            boolean checked = Boolean.valueOf(query.getValue());
            if (checked) {
                params.add(new BasicNameValuePair(query.getKey(), "Y"));
            }
        } else if (query.getSearchField() instanceof DropdownSearchField) {
            params.add(new BasicNameValuePair(query.getKey(), query.getValue()));
        }
        return index;
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException,
            JSONException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> params = new ArrayList<>();

        int index = 0;
        start();

        params.add(new BasicNameValuePair("BOOK", "ON"));
        // params.add(new BasicNameValuePair("MAGAZINE", "ON"));
        // params.add(new BasicNameValuePair("AV", "ON"));

//        index++;
//        index = addParameters(query, KEY_SEARCH_QUERY_FREE, "AB", params, index);
//        index = addParameters(query, KEY_SEARCH_QUERY_AUTHOR, "CD", params,
//                index);
//        index = addParameters(query, KEY_SEARCH_QUERY_PUBLISHER, "EF", params,
//                index);

//        params.add(new BasicNameValuePair("COND", "1")); // condition and
//        params.add(new BasicNameValuePair("SORT", "1")); // sort title order

        // params.add(new BasicNameValuePair("NDC", "  ")); // ndc
        // params.add(new BasicNameValuePair("GENRE", "  ")); // genre
        // params.add(new BasicNameValuePair("BUNRUI", ""));
        // params.add(new BasicNameValuePair("BUNCOMP", "1")); // condition
        // params.add(new BasicNameValuePair("BECCHI", ""));
//        if (query.containsKey(KEY_SEARCH_QUERY_ISBN)
//                && !query.get(KEY_SEARCH_QUERY_ISBN).equals("")) {
//            params.add(new BasicNameValuePair("ISBN", query
//                    .get(KEY_SEARCH_QUERY_ISBN)));
//        }
//        if (query.containsKey(KEY_SEARCH_QUERY_YEAR)
//                && !query.get(KEY_SEARCH_QUERY_YEAR).equals("")) {
//            params.add(new BasicNameValuePair("ITEM9", "P")); // publish from ym
//            params.add(new BasicNameValuePair("KEY9", query
//                    .get(KEY_SEARCH_QUERY_YEAR)));
//            params.add(new BasicNameValuePair("COMP9", "1"));
//        }
        // params.add(new BasicNameValuePair("ITEM10", "Q")); // publish to ym
        // params.add(new BasicNameValuePair("KEY10", ""));
        // params.add(new BasicNameValuePair("COMP10", "1"));

        params.add(new BasicNameValuePair("LIBRARY", "   "));

        // params.add(new BasicNameValuePair("MATER", "   ")); // media type
        // params.add(new BasicNameValuePair("LANG", "   "));
        // params.add(new BasicNameValuePair("CHILD", "   "));
        // params.add(new BasicNameValuePair("TRCUSER", "   "));
        // params.add(new BasicNameValuePair("RANGE", "1"));

        params.add(new BasicNameValuePair("MAXVIEW", "10"));



        for (SearchQuery singleQuery : query) {
            index = addParameters(singleQuery, params, index);
        }

        if (index == 0) {
            throw new OpacErrorException(
                    stringProvider.getString(StringProvider.NO_CRITERIA_INPUT));
        }
        if (index > 5) {
            throw new OpacErrorException(stringProvider.getQuantityString(
                    StringProvider.LIMITED_NUM_OF_CRITERIA, 5, 5));
        }

        String html = httpPost(opac_url + "/clis/search", new UrlEncodedFormEntity(
                        params, getDefaultEncoding()), getDefaultEncoding(), false,
                cookieStore);

        return parse_search(html, 1);
    }

    protected SearchRequestResult parse_search(String html, int page)
            throws OpacErrorException {
        Document doc = Jsoup.parse(html);

        // updateSearchSetValue(doc);

        if (doc.select("div.MSGBOLDL").size() > 0) {
            if (doc.select("div.MSGBOLDL").text().trim()
                   .equals("該当データが見つかりませんでした。")) {
                // nothing found
                return new SearchRequestResult(new ArrayList<SearchResult>(),
                        0, 1, 1);
            } else {
                // error
                throw new OpacErrorException(doc.select("div.MSGBOLDL").first()
                                                .text().trim());
            }
        }

        sid = doc.select("div.BUTTON form input[name=SID]").attr("value");
        tm = doc.select("div.BUTTON form input[name=TM]").attr("value");

        reusehtml = html;

        int results_total = -1;

        String resultnumstr = "";
        if (doc.select("div.PART").size() > 2) {
            resultnumstr = doc.select("div.PART").get(2).text();
        } else if (doc.select("div.PART").size() > 1) {
            resultnumstr = doc.select("div.PART").get(1).text();
        }
        // if ((resultnumstr.indexOf("<BR>") > 0) &&
        // (resultnumstr.indexOf("該当件数") >= 0)) {
        // resultnumstr = resultnumstr.substring(0,
        // resultnumstr.indexOf("<BR>"));
        // }
        // Pattern p = Pattern.compile("[0-9,]+$");
        Pattern p = Pattern.compile("\\b\\d{1,3}(,\\d{3})*\\b");
        Matcher m = p.matcher(resultnumstr);
        if (m.find()) {
            resultnumstr = m.group();
        }
        if (resultnumstr.contains("(")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(",", "")
                                                         .replaceAll(".*\\(([0-9]+)\\).*", "$1"));
        } else if (resultnumstr.contains(": ")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(",", "")
                                                         .replaceAll(".*: ([0-9]+)$", "$1"));
        } else if (resultnumstr.contains("： ")) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(",", "")
                                                         .replaceAll(".*： \\d{1,3}(,\\d{3})*\\b", "$1"));
        } else if (resultnumstr.length() > 0) {
            results_total = Integer.parseInt(resultnumstr.replaceAll(",", ""));
        }

        // end of page
        if ((results_total - 1) / 10 + 1 < page) {
            return new SearchRequestResult(new ArrayList<SearchResult>(), 0, 1,
                    1);
            // return new SearchRequestResult(new ArrayList<SearchResult>(),
            // results_total, (results_total - 1) / 10 + 1, page);
        }
        List<SearchResult> results = new ArrayList<SearchResult>();

        Elements table = doc.select("table.FULL tbody tr");
        // identifier = null;

        Elements links = doc.select("table.FULL a");
        boolean haslink = false;
        for (int i = 0; i < links.size(); i++) {
            Element node = links.get(i);
            if (node.hasAttr("href") & node.attr("href").contains("detail?")
                    && !haslink) {
                haslink = true;
                try {
                    List<NameValuePair> anyurl = URLEncodedUtils.parse(new URI(
                            node.attr("href")), getDefaultEncoding());
                    for (NameValuePair nv : anyurl) {
                        if (nv.getName().equals("identifier")) {
                            // identifier = nv.getValue();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        for (int i = 0; i < table.size(); i++) {
            Element tr = table.get(i);
            SearchResult sr = new SearchResult();
            if (tr.select("td").size() > 1) {
                String fname = tr.select("td").get(1).text();
                if (data.has("mediatypes")) {
                    try {
                        sr.setType(MediaType.valueOf(data.getJSONObject(
                                "mediatypes").getString(fname)));
                    } catch (JSONException e) {
                        sr.setType(defaulttypes.get(fname));
                    } catch (IllegalArgumentException e) {
                        sr.setType(defaulttypes.get(fname));
                    }
                } else {
                    sr.setType(defaulttypes.get(fname));
                }
            }
            // Element middlething = tr.child(2);

            // List<Node> children = middlething.childNodes();
            // int childrennum = children.size();

            StringBuilder description = new StringBuilder();

            if (tr.select("td").size() > 5) {
                description.append("<b>" + tr.select("td").get(2).text()
                        + "</b>");
                description.append("<br />" + tr.select("td").get(3).text());
                description.append(" - " + tr.select("td").get(4).text());
                description.append(" - " + tr.select("td").get(5).text());
                sr.setInnerhtml(description.toString());
            }

            sr.setNr(10 * (page - 1) + i);
            sr.setId(null);
            if (tr.select("td").size() > 1) {
                sr.setId(tr.select("a").attr("href"));
            }
            results.add(sr);
        }
        resultcount = results.size();
        return new SearchRequestResult(results, results_total, page);
    }

    @Override
    public SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException {
        if (!initialised) {
            start();
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("NEXTPAGE", "次の10件を表示　＞"));
        params.add(new BasicNameValuePair("SID", sid));
        params.add(new BasicNameValuePair("TM", tm));
        params.add(new BasicNameValuePair("PCNT", String.valueOf(page - 1)));

        String html = httpPost(opac_url + "/clis/search", new UrlEncodedFormEntity(
                        params, getDefaultEncoding()), getDefaultEncoding(), false,
                cookieStore);
        return parse_search(html, page);
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Option option)
            throws IOException {
        return null;
    }

    @Override
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException {

        if (id == null && reusehtml != null) {
            return parse_result(reusehtml);
        }

        if (!initialised) {
            start();
        }

//        if (id.startsWith("http")) {
//            return parse_result(httpGet(id, getDefaultEncoding()));
//        } else {
//            try {
//                return parse_result(httpGet(opac_url + "/LNG=" + getLang() + "/DB="
//                        + data.getString("db") + "/PPNSET?PPN=" + id, getDefaultEncoding()));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
        String html = httpGet(opac_url.substring(0, opac_url.lastIndexOf("/"))
                + id, getDefaultEncoding(), false);
        return parse_result(html);
    }

    @Override
    public DetailedItem getResult(int position) throws IOException {
        // Should not be called because every media has an ID

        return null;
    }

    protected DetailedItem parse_result(String html) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(opac_url);

        DetailedItem result = new DetailedItem();
        // GET TITLE AND SUBTITLE
        Element part = doc.select("div.PART").first();
        for (Element element : part.select("table tr")) {
            String title = element.select("th").text().trim();
            String detail = element.select("td").text().trim();

            if (title.contains("書名")) {
                result.setTitle(detail);
                // result.addDetail(new Detail("Titelzusatz", subtitle));
            } else if (title.contains("ＩＳＢＮ")) {
                // GET COVER
                if (detail.replaceAll("[^\\dX]", "").length() == 13) {
                    result.setCover(ISBNTools.getAmazonCoverURL(detail, true));
                } else {
                    result.setCover(ISBNTools.getAmazonCoverURL("000" + detail,
                            true));
                }
                result.addDetail(new Detail(title, detail));
            } else {
                result.addDetail(new Detail(title, detail));
            }
        }

        String copyinfo = "";
        if (doc.select("div.PART").size() > 0) {
            part = doc.select("div.PART").get(1);
            if (part.childNodes().size() > 0) {
                for (int i = 0; i < part.childNodes().size(); i++) {
                    copyinfo = copyinfo
                            + part.childNode(i).toString()
                                  .replaceAll("<strong>", "")
                                  .replaceAll("</strong>", "");
                }
            }
            result.addDetail(new Detail("所蔵情報", copyinfo));
        }

        Element form;
        if (doc.select("div.BUTTON").size() > 1) {
            form = doc.select("div.BUTTON form").get(1);
        } else {
            form = doc.select("div.BUTTON form").first();
        }
        result.setId("/detail" + "?NUM="
                + form.select("input[name=NUM]").attr("value") + "&CTG="
                + form.select("input[name=CTG]").attr("value") + "&RTN="
                + form.select("input[name=RTN]").attr("value") + "&SID="
                + form.select("input[name=SID]").attr("value") + "&TM="
                + form.select("input[name=TM]").attr("value"));

        // GET OTHER INFORMATION
        Map<String, String> e = new HashMap<String, String>();

        if (!library.getData().isNull("accountSupported")) {
            try {
                result.setReservable(library.getData().getBoolean(
                        "accountSupported"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        result.setReservation_info(form.attr("action") + "?NUM="
                + form.select("input[name=NUM]").attr("value") + "&CTG="
                + form.select("input[name=CTG]").attr("value") + "&RTN="
                + form.select("input[name=RTN]").attr("value") + "&SID="
                + form.select("input[name=SID]").attr("value") + "&TM="
                + form.select("input[name=TM]").attr("value"));

        if (doc.select("div.PART").size() > 2) {
            part = doc.select("div.PART").get(2);
        }

        return result;
    }

    @Override
    public List<SearchField> parseSearchFields() throws IOException, JSONException {
        if (!initialised) {
            start();
        }

        String html =
                httpGet(opac_url + "/search.html",
                        getDefaultEncoding());
        Document doc = Jsoup.parse(html);
        List<SearchField> fields = new ArrayList<>();

        Elements sort = doc.select("input[name=SORT]");
        if (sort.size() > 0) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(sort.first().parent().parent().parent().parent()
                                     .select("legend").first().text());
            field.setId("SORT");
            for (Element option : sort) {
                field.addDropdownValue(option.attr("value"), option.text());
            }
            fields.add(field);
        }

        for (Element input : doc.select("input[type=text][name^=KEY]")) {
            TextSearchField field = new TextSearchField();
            field.setDisplayName(input.parent().parent().parent().select("legend")
                                      .text());
            field.setId(input.attr("name"));
            field.setData(new JSONObject("{\"ADI\": false}"));
            fields.add(field);
        }

        for (Element dropdown : doc.select("select[name^=ITEM]")) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(dropdown.parent().parent().parent().select("legend")
                    .text());
            field.setId(dropdown.attr("name"));
            for (Element option : dropdown.select("option")) {
                field.addDropdownValue(option.attr("value"), option.text());
            }
            fields.add(field);
        }

        Elements fuzzy = doc.select("input[name=COND]");
        if (fuzzy.size() > 0) {
            CheckboxSearchField field = new CheckboxSearchField();
            field.setDisplayName(fuzzy.first().parent().parent().parent().parent()
                                      .select("legend").first().text());
            field.setId("COND");
            fields.add(field);
        }

        Elements mediatypes = doc.select("select[name=MATER]");
        if (mediatypes.size() > 0) {
            DropdownSearchField field = new DropdownSearchField();
            field.setDisplayName(mediatypes.first().parent().parent().select("legend")
                                      .text());
            field.setId("MATER");

            field.addDropdownValue("", "すべて");
            for (Element mt : mediatypes.select("option")) {
                field.addDropdownValue(mt.attr("value"),
                        mt.parent().nextElementSibling().text());
//                        mt.parent().nextElementSibling().text().replace("\u00a0", ""));
            }
            fields.add(field);
        }

        return fields;
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        String reservation_info = item.getReservation_info();

        Document doc = null;

        if (useraction == MultiStepResult.ACTION_CONFIRMATION) {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(9);
            nameValuePairs.add(new BasicNameValuePair("LCOD", lcod));
            if (contact != null) {
                nameValuePairs.add(new BasicNameValuePair("CONTACT", contact));
            }
            nameValuePairs.add(new BasicNameValuePair("NUM", num));
            nameValuePairs.add(new BasicNameValuePair("CTG", ctg));
            nameValuePairs.add(new BasicNameValuePair("SHP", shp));
            nameValuePairs.add(new BasicNameValuePair("RTN", rtn));
            nameValuePairs.add(new BasicNameValuePair("SID", sid));
            nameValuePairs.add(new BasicNameValuePair("TM", tm));
            nameValuePairs.add(new BasicNameValuePair("AUT", aut));
            httpPost(opac_url + "/reserv", new UrlEncodedFormEntity(
                    nameValuePairs), getDefaultEncoding());
            return new ReservationResult(MultiStepResult.Status.OK);
        } else if (selection == null || useraction == 0) {
            // select branch
            String html = httpGet(reservation_info, getDefaultEncoding());
            doc = Jsoup.parse(html);

            if (doc.select("select[name=CONTACT]").size() > 0) {
                List<Map<String, String>> branches = new ArrayList<>();
                for (Element option : doc.select("select[name=CONTACT]")
                                         .first().children()) {
                    String value = option.text().trim();
                    String key;
                    if (option.hasAttr("value")) {
                        key = option.attr("value");
                    } else {
                        key = value;
                    }
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", key);
                    selopt.put("value", value);
                    branches.add(selopt);
                }
                ReservationResult result = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED);
                result.setActionIdentifier(ReservationResult.ACTION_USER);
                result.setSelection(branches);
                return result;
            } else if (doc.select("select[name=LCOD]").size() > 0) {
                List<Map<String, String>> branches = new ArrayList<>();
                for (Element option : doc.select("select[name=LCOD]").first()
                                         .children()) {
                    String value = option.text().trim();
                    String key;
                    if (option.hasAttr("value")) {
                        key = option.attr("value");
                    } else {
                        key = value;
                    }
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", key);
                    selopt.put("value", value);
                    branches.add(selopt);
                }
                ReservationResult result = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED);
                result.setActionIdentifier(ReservationResult.ACTION_BRANCH);
                result.setSelection(branches);
                return result;
            }
        } else if (useraction == ReservationResult.ACTION_USER) {
            // select branch
            contact = selection;
            String html = httpGet(reservation_info, getDefaultEncoding());
            doc = Jsoup.parse(html);

            if (doc.select("select[name=LCOD]").size() > 0) {
                List<Map<String, String>> branches = new ArrayList<>();
                for (Element option : doc.select("select[name=LCOD]").first()
                                         .children()) {
                    String value = option.text().trim();
                    String key;
                    if (option.hasAttr("value")) {
                        key = option.attr("value");
                    } else {
                        key = value;
                    }
                    Map<String, String> selopt = new HashMap<>();
                    selopt.put("key", key);
                    selopt.put("value", value);
                    branches.add(selopt);
                }
                ReservationResult result = new ReservationResult(
                        MultiStepResult.Status.SELECTION_NEEDED);
                result.setActionIdentifier(ReservationResult.ACTION_BRANCH);
                result.setSelection(branches);
                return result;
            }
        } else if (useraction == ReservationResult.ACTION_BRANCH) {
            String html = httpGet(reservation_info, getDefaultEncoding());
            doc = Jsoup.parse(html);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
                    10);
            nameValuePairs
                    .add(new BasicNameValuePair("UID", account.getName()));
            nameValuePairs.add(new BasicNameValuePair("PASS", account
                    .getPassword()));
            if (contact != null) {
                nameValuePairs.add(new BasicNameValuePair("CONTACT", contact));
            }
            nameValuePairs.add(new BasicNameValuePair("LCOD", selection));
            nameValuePairs.add(new BasicNameValuePair("NUM", doc.select(
                    "input[name=NUM]").attr("value")));
            nameValuePairs.add(new BasicNameValuePair("CTG", doc.select(
                    "input[name=CTG]").attr("value")));
            nameValuePairs.add(new BasicNameValuePair("SHP", doc.select(
                    "input[name=SHP]").attr("value")));
            nameValuePairs.add(new BasicNameValuePair("RTN", doc.select(
                    "input[name=RTN]").attr("value")));
            nameValuePairs.add(new BasicNameValuePair("SID", doc.select(
                    "input[name=SID]").attr("value")));
            nameValuePairs.add(new BasicNameValuePair("TM", doc.select(
                    "input[name=TM]").attr("value")));
            html = httpPost(opac_url + "/entcnf", new UrlEncodedFormEntity(
                    nameValuePairs), getDefaultEncoding());
            doc = Jsoup.parse(html);
        }

        if (doc == null)
            return new ReservationResult(MultiStepResult.Status.ERROR);

        if (doc.select("div.PART table tr").size() > 0) {
            List<String[]> details = new ArrayList<String[]>();

            contact = doc.select("input[name=CONTACT]").attr("value");
            lcod = doc.select("input[name=LCOD]").attr("value");
            num = doc.select("input[name=NUM]").attr("value");
            ctg = doc.select("input[name=CTG]").attr("value");
            shp = doc.select("input[name=SHP]").attr("value");
            rtn = doc.select("input[name=RTN]").attr("value");
            sid = doc.select("input[name=SID]").attr("value");
            tm = doc.select("input[name=TM]").attr("value");
            aut = doc.select("input[name=AUT]").attr("value");

            for (Element row : doc.select("div.PART table tr")) {
                if (row.select("th").size() == 1
                        && row.select("td").size() == 1) {
                    details.add(new String[] { row.select("th").text().trim(),
                            row.select("td").text().trim() });
                }
            }
            ReservationResult result = new ReservationResult(
                    MultiStepResult.Status.CONFIRMATION_NEEDED);
            result.setDetails(details);
            return result;
        }

        if (doc.getElementsByClass("MSGBOLDL").size() == 1) {
            return new ReservationResult(MultiStepResult.Status.ERROR, doc
                    .getElementsByClass("MSGBOLDL").get(0).text());
        }

        return new ReservationResult(MultiStepResult.Status.ERROR,
                "Unbekannter Fehler");
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String Selection) throws IOException {
        if (!initialised) {
            start();
        }
        if (pwEncoded == null)
            try {
                account(account);
            } catch (JSONException e1) {
                return new ProlongResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e1) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e1.getMessage());
            }

        String html = httpGet(media, getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        if (doc.getElementsByClass("MSGBOLDL").size() == 1) {
            ProlongResult res = new ProlongResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.getElementsByClass("MSGBOLDL").text());
            return res;
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("AUT", doc.select(
                "div.BUTTON input[name=AUT]").attr("value")));
        params.add(new BasicNameValuePair("SID", doc.select(
                "div.BUTTON input[name=SID]").attr("value")));
        params.add(new BasicNameValuePair("TM", doc.select(
                "div.BUTTON input[name=TM]").attr("value")));
        params.add(new BasicNameValuePair("CONUM", doc.select(
                "div.BUTTON input[name=CONUM]").attr("value")));
        params.add(new BasicNameValuePair("RTN", doc.select(
                "div.BUTTON input[name=RTN]").attr("value")));
        params.add(new BasicNameValuePair("LIM", doc.select(
                "div.BUTTON input[name=LIM]").attr("value")));

        html = httpPost(doc.select("div.BUTTON form").attr("action"),
                new UrlEncodedFormEntity(params, getDefaultEncoding()),
                getDefaultEncoding());
        doc = Jsoup.parse(html);

        if (doc.getElementsByClass("MSGBOLDL").size() == 1) {
            ProlongResult res = new ProlongResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.getElementsByClass("MSGBOLDL").text());
            return res;
        }

        if (doc.select("form input[name=PASS]").size() > 0) {
            params.add(new BasicNameValuePair("PASS", account.getPassword()));
        }

        html = httpPost(doc.select("div.BUTTON form").attr("action"),
                new UrlEncodedFormEntity(params, getDefaultEncoding()),
                getDefaultEncoding());
        doc = Jsoup.parse(html);

        if (doc.text().contains("延長しました")) {
            return new ProlongResult(MultiStepResult.Status.OK);
        } else if (doc.select("div.MSGBOLDL").text().contains("もう一度ログインしてください")
                || doc.select("div.MSGBOLDL").text().contains("有効時間が経過しています")) {
            try {
                account(account);
                return prolong(media, account, useraction, Selection);
            } catch (JSONException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR);
            } catch (OpacErrorException e) {
                return new ProlongResult(MultiStepResult.Status.ERROR,
                        e.getMessage());
            }
        } else {
            ProlongResult res = new ProlongResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.select("div.MSGBOLDL").text());
            return res;
        }
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        if (!initialised) {
            start();
        }
        String html = httpGet(media, getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        if (doc.select("div.MSGBOLDL").size() > 0) {
            CancelResult res = new CancelResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.select("div.MSGBOLDL").text());
            return res;
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("LCOD", doc.select(
                "form select[name=LCOD] option[selected]").val()));
        params.add(new BasicNameValuePair("CONTACT", doc.select(
                "form select[name=CONTACT] option[selected]").val()));
        params.add(new BasicNameValuePair("5", "予約を取り消す"));
        params.add(new BasicNameValuePair("CTG", doc.select(
                "form input[name=CTG]").attr("value")));
        params.add(new BasicNameValuePair("RSNUM", doc.select(
                "form input[name=RSNUM]").attr("value")));
        params.add(new BasicNameValuePair("AUT", doc.select(
                "form input[name=AUT]").attr("value")));
        params.add(new BasicNameValuePair("SID", doc.select(
                "form input[name=SID]").attr("value")));
        params.add(new BasicNameValuePair("TM", doc.select(
                "form input[name=TM]").attr("value")));
        params.add(new BasicNameValuePair("RTN", doc.select(
                "form input[name=RTN]").attr("value")));

        html = httpPost(doc.select("form").attr("action"),
                new UrlEncodedFormEntity(params, getDefaultEncoding()),
                getDefaultEncoding());
        doc = Jsoup.parse(html);

        if (doc.select("div.MSGBOLDL").size() > 0) {
            CancelResult res = new CancelResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.select("div.MSGBOLDL").text());
            return res;
        }

        if (doc.select("form input[name=PASS]").size() > 0) {
            params.add(new BasicNameValuePair("PASS", account.getPassword()));
        }

        html = httpPost(doc.select("form").attr("action"),
                new UrlEncodedFormEntity(params, getDefaultEncoding()),
                getDefaultEncoding());
        doc = Jsoup.parse(html);

        if (doc.text().contains("取り消しました")) {
            return new CancelResult(MultiStepResult.Status.OK);
        } else if (doc.select("div.MSGBOLDL").text().contains("もう一度ログインしてください")
                || doc.select("div.MSGBOLDL").text().contains("有効時間が経過しています")) {
            try {
                account(account);
                return cancel(media, account, useraction, selection);
            } catch (JSONException e) {
                throw new OpacErrorException("内部エラー");
            }
        } else {
            CancelResult res = new CancelResult(MultiStepResult.Status.ERROR);
            res.setMessage(doc.select("div.MSGBOLDL").text());
            return res;
        }
    }

    @Override
    public AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("UID", account.getName()));
        params.add(new BasicNameValuePair("PASS", account.getPassword()));

        String html = httpPost(opac_url + "/login", new UrlEncodedFormEntity(
                params, getDefaultEncoding()), getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        // if (doc.select("div.MSGBOLDL").size() > 0) {
        // throw new OpacErrorException(doc.select("div.MSGBOLDL").text());
        // }
        if (doc.select("input[name=AUT]").size() > 0) {
            pwEncoded = URLEncoder.encode(
                    doc.select("input[name=AUT]").attr("value"), "UTF-8");
        } else {
            // TODO: do something here to help fix bug #229
        }

        rtn = doc.select("input[name=RTN]").attr("value");
        sid = doc.select("input[name=SID]").attr("value");
        tm = doc.select("input[name=TM]").attr("value");
        aut = doc.select("input[name=AUT]").attr("value");

        html = httpGet(opac_url + "/logrent?AUT=" + aut + "&MAXVIEW=10&SID="
                + sid + "&TM=" + tm, getDefaultEncoding());
        doc = Jsoup.parse(html);

        html = httpGet(opac_url + "/logrsrv?AUT=" + aut + "&MAXVIEW=10&SID="
                + sid + "&TM=" + tm, getDefaultEncoding());
        Document doc2 = Jsoup.parse(html);

        AccountData res = new AccountData(account.getId());

        List<LentItem> media = new ArrayList<>();
        List<ReservedItem> reservations = new ArrayList<>();
        if (doc.select("table tbody tr").size() > 0) {
            parse_medialist(media, doc, 1, account.getName());
        }
        if (doc2.select("table tbody tr").size() > 0) {
            parse_reslist(reservations, doc2, 1);
        }

        res.setLent(media);
        res.setReservations(reservations);

        if (media == null || reservations == null) {
            throw new OpacErrorException("不明なエラー. アカウントが正しいことを確認してください.");
            // Log.d("OPACCLIENT", html);
        }
        return res;

    }

    protected void parse_medialist(List<LentItem> medien,
            Document doc, int offset, String accountName)
            throws ClientProtocolException, IOException {

        Elements copytrs = doc.select("table > tbody > tr");
        Elements table = doc.select("div.PART table thead tr th");

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy年M月d日").withLocale(Locale.JAPAN);

        int trs = copytrs.size();
        if (trs < 1) {
            medien = null;
            return;
        }
        assert (trs > 0);
        for (int i = 0; i < trs; i++) {
            Document doc1 = null;
            try {
                String html = httpGet(copytrs.get(i).select("a").attr("href"),
                        getDefaultEncoding());
                doc1 = Jsoup.parse(html);
            } catch (IOException e) {

            }
            Element tr = copytrs.get(i);
            LentItem item = new LentItem();
            for (int j = 0; j < table.size(); j++) {
                if (table.get(j).text().trim().equals("書名")
                        || table.get(j).text().trim().equals("タイトル")) {
                    item.setTitle(tr.select("td").get(j).text().trim());
                } else if (table.get(j).text().trim().equals("返却期限")) {
                    item.setDeadline(tr.select("td").get(j).text().trim());
                    try {
                        item.setDeadline(fmt.parseLocalDate(tr.select("td").get(j).text().trim()));
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            if (doc1.select("div.BUTTON form").size() > 0)
                item.setProlongData(copytrs.select("a").attr("href"));
            else
                item.setRenewable(false);

            medien.add(item);
        }
        assert (medien.size() == trs - 1);
    }

    protected void parse_reslist(List<ReservedItem> medien,
            Document doc, int offset) throws ClientProtocolException,
            IOException {

        // if(doc.select("input[name=LOR_RESERVATIONS]").size()>0) {
        // lor_reservations =
        // doc.select("input[name=LOR_RESERVATIONS]").attr("value");
        // }

        Elements copytrs = doc.select("div.PART table tbody tr");
        Elements table = doc.select("div.PART table thead tr th");

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy年M月d日").withLocale(Locale.JAPAN);

        int trs = copytrs.size();
        if (trs < 1) {
            medien = null;
            return;
        }
        assert (trs > 0);
        for (int i = 0; i < trs; i++) {
            Element tr = copytrs.get(i);
            ReservedItem item = new ReservedItem();

            for (int j = 0; j < table.size(); j++) {
                if (table.get(j).text().trim().equals("書名")
                        || table.get(j).text().trim().equals("タイトル")) {
                    item.setTitle(tr.select("td").get(j).text().trim());
                } else if (table.get(j).text().trim().equals("予約状況")) {
                    item.setReadyDate(tr.select("td").get(j).text().trim());
                    try {
                        item.setReadyDate(fmt.parseLocalDate(tr.select("td").get(j).text().trim()));
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            // e.put(AccountData.KEY_RESERVATION_TITLE,
            // tr.select("td").get(1).text().trim());
            // e.put(AccountData.KEY_RESERVATION_READY,
            // tr.select("td").get(4).text().trim());
            item.setBookingData(tr.select("a").attr("href"));

            medien.add(item);
        }
        assert (medien.size() == trs - 1);
    }

    @Override
    public String getShareUrl(String id, String title) {
//        if (id.startsWith("http")) {
//            return id;
//        } else {
//            return opac_url + "/LNG=" + getLang() + "/DB=" + db + "/PPNSET?PPN=" + id;
//        }
        return id;
    }

    @Override
    public int getSupportFlags() {
        return SUPPORT_FLAG_ENDLESS_SCROLLING | SUPPORT_FLAG_CHANGE_ACCOUNT;
    }

    public void updateSearchSetValue(Document doc) {
        String url = doc.select("base").first().attr("href");
        Integer setPosition = url.indexOf("SET=") + 4;
        String searchSetString = url.substring(setPosition,
                url.indexOf("/", setPosition));
        searchSet = Integer.parseInt(searchSetString);
    }

    @Override
    public void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException {
        start();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("UID", account.getName()));
        params.add(new BasicNameValuePair("PASS", account.getPassword()));

        String html = httpPost(opac_url + "/login", new UrlEncodedFormEntity(
                params, getDefaultEncoding()), getDefaultEncoding());
        Document doc = Jsoup.parse(html);

        // if (doc.select("div.MSGBOLDL").size() > 0) {
        // throw new OpacErrorException(doc.select("div.MSGBOLDL").text());
        // }
        if (doc.select("input[name=AUT]").size() > 0) {
            pwEncoded = URLEncoder.encode(
                    doc.select("input[name=AUT]").attr("value"), "UTF-8");
        } else {
            throw new OpacErrorException(doc.select(".kontomeldung").text());
        }
    }

    @Override
    protected String getDefaultEncoding() {
        try {
            if (data.has("charset")) {
                return data.getString("charset");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "UTF-8";
    }

    @Override
    public void setLanguage(String language) {
        this.languageCode = language;
    }

    protected String getLang() {
        if (!initialised) {
            return null;
        }
        if (supportedLanguages.contains(languageCode)) {
            return languageCodes.get(languageCode);
        } else if (supportedLanguages.contains("en"))
        // Fall back to English if language not available
        {
            return languageCodes.get("en");
        } else if (supportedLanguages.contains("ja"))
        // Fall back to Japanese if English not available
        {
            return languageCodes.get("ja");
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        Set<String> langs = new HashSet<>();
        langs.add("ja");
        return langs;
    }
}
