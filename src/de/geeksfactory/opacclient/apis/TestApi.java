package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Filter.Option;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;

public class TestApi extends BaseApi implements OpacApi {

	@Override
	public void start() throws IOException, NotReachableException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SearchRequestResult search(Map<String, String> query)
			throws IOException, NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchRequestResult filterResults(Filter filter, Option option)
			throws IOException, NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DetailledItem getResult(int position) throws IOException,
			OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReservationResult reservation(DetailledItem item, Account account,
			int useraction, String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProlongResult prolong(String media, Account account, int useraction,
			String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProlongAllResult prolongAll(Account account, int useraction,
			String selection) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CancelResult cancel(String media, Account account, int useraction,
			String selection) throws IOException, OpacErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccountData account(Account account) throws IOException,
			JSONException, OpacErrorException {
		AccountData res = new AccountData(account.getId());
		List<Map<String, String>> lent = new ArrayList<Map<String, String>>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		Calendar cal = Calendar.getInstance();
		
		Map<String, String> media1 = new HashMap<String, String>();
		media1.put(AccountData.KEY_LENT_AUTHOR, "Mustermann, Max");
		media1.put(AccountData.KEY_LENT_DEADLINE, sdf.format(cal.getTime()));
		media1.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String.valueOf(cal.getTimeInMillis()));
		media1.put(AccountData.KEY_LENT_RENEWABLE, "N");
		media1.put(AccountData.KEY_LENT_TITLE, "Musterbuch");
		media1.put(AccountData.KEY_LENT_STATUS, "Status");
		lent.add(media1);
		
		cal.add(Calendar.DATE, 2);
		
		Map<String, String> media2 = new HashMap<String, String>();
		media2.put(AccountData.KEY_LENT_AUTHOR, "Mustermann, Martina");
		media2.put(AccountData.KEY_LENT_DEADLINE, sdf.format(cal.getTime()));
		media2.put(AccountData.KEY_LENT_DEADLINE_TIMESTAMP, String.valueOf(cal.getTimeInMillis()));
		media2.put(AccountData.KEY_LENT_RENEWABLE, "N");
		media2.put(AccountData.KEY_LENT_TITLE, "Musterbuch 2");
		media2.put(AccountData.KEY_LENT_STATUS, "Status");
		lent.add(media2);
		
		res.setLent(lent);
		res.setReservations(new ArrayList<Map<String, String>>());
		return res;
	}

	@Override
	public String[] getSearchFields() {
		return new String[]{};
	}

	@Override
	public boolean isAccountSupported(Library library) {
		return true;
	}

	@Override
	public boolean isAccountExtendable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getShareUrl(String id, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSupportFlags() {
		return SUPPORT_FLAG_ENDLESS_SCROLLING;
	}

}
