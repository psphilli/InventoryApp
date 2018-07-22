package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventory.data.ProductContract.ProductEntry;
import com.example.android.inventory.data.ProductCursorAdapter;

/**
 * Displays list of products that were entered and stored in the by the inventory app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = CatalogActivity.class.getName();

    // This is the Adapter being used to display the list's data.
    private ProductCursorAdapter mProductCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, com.example.android.inventory.EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the product data
        ListView productListView = findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Setup cursor adapter using cursor from last step
        mProductCursorAdapter = new ProductCursorAdapter(this, null);
        // Attach cursor adapter to the ListView
        productListView.setAdapter(mProductCursorAdapter);

        // Setup item click listener
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Create net intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, com.example.android.inventory.EditorActivity.class);

                // Form the content URI that represents the specific product that was clicked on,
                // by appending the passed id onto the {@link ProductEntry#CONTENT_URI}
                Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentProductUri);

                // Launch the {@link EditorActivity} to display data for the current product
                startActivity(intent);
            }
        });


        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add menu items to the app bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert Product" menu option
            case R.id.action_insert_product:
                insertProduct();
                return true;
            // Respond to a click on the "Delete All Products" menu option
            case R.id.action_delete_products:
                deleteProducts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //region implementation of LoaderManager.LoaderCallbacks

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = new String[] {
            ProductEntry._ID,
            ProductEntry.COLUMN_PRODUCT_NAME,
            ProductEntry.COLUMN_PRODUCT_PRICE,
            ProductEntry.COLUMN_PRODUCT_QUANTITY,
        };

        // Create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this,
        ProductEntry.CONTENT_URI,
        projection,
        null,
        null,
        null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mProductCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mProductCursorAdapter.swapCursor(null);
    }

    //endregion

    /**
     * Helper method to insert hardcoded product data into the products table.
     */
    private void insertProduct() {

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, getString(R.string.street_rod_name));
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, Float.parseFloat(getString(R.string.street_rod_price)));
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, Integer.parseInt(getString(R.string.street_rod_quantity)));
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME, getString(R.string.street_rod_supplier_name));
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_PHONE, getString(R.string.street_rod_supplier_phone));

        // Insert the new row, returning the primary key value of the new row
        try {
            Uri uri = getContentResolver().insert(
                    ProductEntry.CONTENT_URI,
                    values);

            Log.i(LOG_TAG, "The uri of the newly inserted row is: " + uri);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "There was a problem inserting the product", ex);
        }
    }

    /**
     * Helper method to delete all products table rows.
     */
    private void deleteProducts() {
        // Delete all rows returning the number deleted
        try {
            int numberDeleted = getContentResolver().delete(
                    ProductEntry.CONTENT_URI,
                    null,
                    null);

            Log.i(LOG_TAG, "Deleted rows: " + numberDeleted);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "There was a problem deleting the products", ex);
        }
    }
}
