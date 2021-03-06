package de.geeksfactory.opacclient.frontend;

import android.os.Bundle;
import android.view.MenuItem;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;

/**
 * An activity representing a single SearchResult detail screen. This activity
 * is only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a
 * {@link SearchResultListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link SearchResultDetailFragment}.
 */
public class SearchResultDetailActivity extends OpacActivity implements SearchResultDetailFragment.Callbacks {

	private OpacClient app;
	SearchResultDetailFragment detailFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = (OpacClient) getApplication();

		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putInt(
					SearchResultDetailFragment.ARG_ITEM_NR,
					getIntent().getIntExtra(
							SearchResultDetailFragment.ARG_ITEM_NR, 0));
			if(getIntent().getStringExtra(
					SearchResultDetailFragment.ARG_ITEM_ID) != null)
				arguments.putString(
						SearchResultDetailFragment.ARG_ITEM_ID,
						getIntent().getStringExtra(
								SearchResultDetailFragment.ARG_ITEM_ID));
			detailFragment = new SearchResultDetailFragment();
			detailFragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.searchresult_detail_container, detailFragment).commit();
		}
	}

	@Override
	public void removeFragment() {
		finish();
	}

	@Override
	protected int getContentView() {
		return R.layout.activity_searchresult_detail;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() ==  android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
