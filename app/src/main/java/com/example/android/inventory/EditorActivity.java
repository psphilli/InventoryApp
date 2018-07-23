package com.example.android.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.ProductContract.ProductEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    ///region Fields

    private static final String LOG_TAG = EditorActivity.class.getName();

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the product's price */
    private EditText mPriceEditText;

    /** EditText field to enter the quantity of product's */
    private TextView mQuantityTextView;

    /** EditText field to enter the product supplier's name */
    private EditText mSupplierNameEditText;

    /** EditText field to enter the product supplier's phone number */
    private EditText mSupplierPhoneEditText;

    // During edit mode, this stores the Uri of the product being changed
    private Uri mCurrentProductUri;

    // Edit dirty flag to avoid missing edits before save is clicked
    private boolean mProductHasChanged = false;

    ///endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity
        // in order to figure out if were creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // adding a product
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        }
        else {
            // editing a product
            setTitle(getString(R.string.editor_activity_title_edit_product));

            getLoaderManager().initLoader(0, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = findViewById(R.id.edit_product_name);
        mPriceEditText = findViewById(R.id.edit_product_price);
        mQuantityTextView = findViewById(R.id.quantity);
        mSupplierNameEditText = findViewById(R.id.edit_product_supplier_name);
        mSupplierPhoneEditText = findViewById(R.id.edit_product_supplier_phone);

        mPriceEditText.setFilters(new InputFilter[] {new CurrencyFormatInputFilter()});

        // Listen for unsaved edits
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityTextView.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierPhoneEditText.setOnTouchListener(mTouchListener);

        // Setup click handler for increment quantity button
        Button increment = findViewById(R.id.increment_age);
        increment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int age = Integer.parseInt(mQuantityTextView.getText().toString());
                mQuantityTextView.setText(String.valueOf(++age));
            }
        });

        // Setup click handler for decrement quantity button
        Button decrement = findViewById(R.id.decrement_age);
        decrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int age = Integer.parseInt(mQuantityTextView.getText().toString());
                // Make sure doesn't go negative
                if (--age >= 0)
                    mQuantityTextView.setText(String.valueOf(age));
            }
        });

        // Setup click handler for order button
        Button order = findViewById(R.id.order);
        order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Uri uri = Uri.parse("tel:" + mSupplierPhoneEditText.getText().toString());
            PackageManager packageManager = v.getContext().getPackageManager();
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), getString(R.string.phone_app_not_available), Toast.LENGTH_SHORT).show();
            }
            startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                if (saveProduct()) {
                    finish();
                }
                break;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // User clicked "Discard" button, navigate to parent activity.
                            NavUtils.navigateUpFromSameTask(EditorActivity.this);
                        }
                    };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
            new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked "Discard" button, close the current activity.
                finish();
            }
        };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /** OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    //region implementation of LoaderManager.LoaderCallbacks

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = new String[] {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_PHONE,
        };

        // Create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (data == null || data.getCount() < 1) {
            return;
        }

        data.moveToNext();

        mNameEditText.setText(
            data.getString(data.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_NAME)));
        mPriceEditText.setText(
            String.valueOf(data.getFloat(data.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_PRICE))));
        mQuantityTextView.setText(
            String.valueOf(data.getInt(data.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_QUANTITY))));
        mSupplierNameEditText.setText(
            data.getString(data.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME)));
        mSupplierPhoneEditText.setText(
            PhoneNumberUtils.formatNumber(
                data.getString(data.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_SUPPLIER_PHONE))
            ));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        final String EMPTY_STRING = "";

        mNameEditText.setText(EMPTY_STRING);
        mPriceEditText.setText(EMPTY_STRING);
        mQuantityTextView.setText(EMPTY_STRING);
        mSupplierNameEditText.setText(EMPTY_STRING);
        mSupplierPhoneEditText.setText(EMPTY_STRING);
    }

    //endregion

    //region Helpers

    private boolean saveProduct()
    {
        String name = mNameEditText.getText().toString().trim();
        String price = mPriceEditText.getText().toString().trim();
        String quantity = mQuantityTextView.getText().toString().trim();
        String supplierName = mSupplierNameEditText.getText().toString().trim();
        String supplierPhone = mSupplierPhoneEditText.getText().toString().trim();

        // See if nothing has been entered
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(price) && TextUtils.isEmpty(quantity)
                && TextUtils.isEmpty(supplierName) && TextUtils.isEmpty(supplierPhone)) {
            // Don't save
            return true;
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME, supplierName);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_PHONE, supplierPhone);

        boolean success = false;
        try {
            if (mCurrentProductUri == null) {
            // Insert the new row, returning the primary key value of the new row
                 Uri uri = getContentResolver().insert(
                        ProductEntry.CONTENT_URI,
                        values);

                if (uri != null)
                    success = true;
            }
            else {
                // Update the existing row, returning the primary key value of the existing row
                int rowsUpdated;
                rowsUpdated = getContentResolver().update(
                        mCurrentProductUri,
                        values,
                        null,
                        null
                );

                if (rowsUpdated == 1)
                    success = true;
            }
        }
        catch (IllegalArgumentException ex) {
            Toast toast = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return false;
        }
        catch (Exception ex) {
            Log.e(LOG_TAG, "There was a problem with saving product: ", ex);
            Toast toast = Toast.makeText(this, R.string.editor_product_save_failed, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return false;
        }

        // Show a toast message depending on whether or not the insert/update was successful
        String message = getString(R.string.editor_product_save_successful);
        if (!success) {
            getString(R.string.editor_product_save_failed);
        }
        Toast.makeText(this, message,
                Toast.LENGTH_SHORT).show();

        return success;
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            // User clicked the "Keep editing" button, so dismiss the dialog
            // and continue editing the product.
            if (dialog != null) {
                dialog.dismiss();
            }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        int rowsDeleted = getContentResolver().delete(
                mCurrentProductUri,
                null,
                null
        );

        // Show a toast message depending if the delete was successful
        String message = getString(R.string.editor_delete_product_successful);
        if (rowsDeleted != 1)
            getString(R.string.editor_delete_product_failed);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //endregion

    class CurrencyFormatInputFilter implements InputFilter {

        final Pattern mPattern = Pattern.compile("(0|[1-9]+[0-9]*)?(\\.[0-9]{0,2})?");

        @Override
        public CharSequence filter(
                CharSequence source,
                int start,
                int end,
                Spanned dest,
                int dstart,
                int dend) {

            String result =
                    dest.subSequence(0, dstart)
                            + source.toString()
                            + dest.subSequence(dend, dest.length());

            Matcher matcher = mPattern.matcher(result);

            if (!matcher.matches()) return dest.subSequence(dstart, dend);

            return null;
        }
    }
}