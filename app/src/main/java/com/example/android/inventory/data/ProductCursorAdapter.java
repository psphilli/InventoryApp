package com.example.android.inventory.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.R;
import com.example.android.inventory.data.ProductContract.ProductEntry;

import java.text.NumberFormat;
import java.util.Locale;

public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);

        return view;
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get the view holder which has references to the list_item resource identifiers
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Extract properties from cursor
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry._ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_NAME));
        Float price = cursor.getFloat(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_PRICE));
        int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_QUANTITY));

        // Populate fields with extracted properties
        viewHolder.nameView.setText(name);
        viewHolder.priceView.setText(displayCurrency(price));
        viewHolder.quantityView.setText(String.valueOf(quantity));
        viewHolder.saleView.setTag(id);

        viewHolder.saleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current quantity from the sibling text view
                View parent = (View) v.getParent();
                ViewHolder viewHolder = (ViewHolder) parent.getTag();
                int quantity = Integer.parseInt(viewHolder.quantityView.getText().toString());

                // If there is no more of this product in inventory, display message and leave
                if (quantity == 0) {
                    Toast.makeText(v.getContext(), v.getContext().getString(R.string.product_not_available), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get the product's unique row identifier stored in the button's tag
                int id = (int)v.getTag();

                // Form the content URI that represents the specific product that was clicked on,
                // by appending the id onto the {@link ProductEntry#CONTENT_URI}
                Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                // Decrement quantity
                ContentValues values = new ContentValues();
                values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, --quantity);

                // Make the update
                try {
                    v.getContext().getContentResolver().update(
                            currentProductUri,
                            values,
                            null,
                            null
                    );
                }
                catch (IllegalArgumentException ex) {
                    Toast.makeText(v.getContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     *  Nested class that provides implementation of the view holder pattern.
     *  It is used to persist the list_item resource identifiers so that they are only
     *  found {@link View#findViewById} once.
     */
    static class ViewHolder {
        private final TextView nameView;
        private final TextView priceView;
        private final TextView quantityView;
        private final Button saleView;

        private ViewHolder(View view)
        {
            nameView = view.findViewById(R.id.name);
            priceView = view.findViewById(R.id.price);
            quantityView = view.findViewById(R.id.quantity);
            saleView = view.findViewById(R.id.sale);
        }
    }

    /**
     * Use the device's locale to format the given amount as currency
     *
     * @param amount the value to be formatted as currency
     * @return the string representation of the given value formatted as currency
     */
    private static String displayCurrency(Float amount) {
        NumberFormat currencyFormatter =
                NumberFormat.getCurrencyInstance(Locale.getDefault());

        return currencyFormatter.format(amount);
    }
}
