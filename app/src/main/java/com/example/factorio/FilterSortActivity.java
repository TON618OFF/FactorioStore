package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class FilterSortActivity extends AppCompatActivity {

    private CheckBox inStockCheckbox;
    private RadioGroup priceSortGroup;
    private RadioGroup quantitySortGroup;
    private MaterialButton applyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_sort);

        inStockCheckbox = findViewById(R.id.in_stock_checkbox);
        priceSortGroup = findViewById(R.id.price_sort_group);
        quantitySortGroup = findViewById(R.id.quantity_sort_group);
        applyButton = findViewById(R.id.apply_button);

        // Получаем текущие настройки из Intent (если есть)
        Intent intent = getIntent();
        inStockCheckbox.setChecked(intent.getBooleanExtra("inStock", false));
        String priceSort = intent.getStringExtra("priceSort");
        String quantitySort = intent.getStringExtra("quantitySort");

        if ("asc".equals(priceSort)) {
            priceSortGroup.check(R.id.price_asc_radio);
        } else if ("desc".equals(priceSort)) {
            priceSortGroup.check(R.id.price_desc_radio);
        } else {
            priceSortGroup.check(R.id.price_none_radio);
        }

        if ("asc".equals(quantitySort)) {
            quantitySortGroup.check(R.id.quantity_asc_radio);
        } else if ("desc".equals(quantitySort)) {
            quantitySortGroup.check(R.id.quantity_desc_radio);
        } else {
            quantitySortGroup.check(R.id.quantity_none_radio);
        }

        applyButton.setOnClickListener(v -> applyFiltersAndSort());
    }

    private void applyFiltersAndSort() {
        Intent resultIntent = new Intent();

        // Фильтр "В наличии"
        boolean inStock = inStockCheckbox.isChecked();
        resultIntent.putExtra("inStock", inStock);

        // Сортировка по цене
        String priceSort = "none";
        int priceCheckedId = priceSortGroup.getCheckedRadioButtonId();
        if (priceCheckedId == R.id.price_asc_radio) {
            priceSort = "asc";
        } else if (priceCheckedId == R.id.price_desc_radio) {
            priceSort = "desc";
        }
        resultIntent.putExtra("priceSort", priceSort);

        // Сортировка по количеству
        String quantitySort = "none";
        int quantityCheckedId = quantitySortGroup.getCheckedRadioButtonId();
        if (quantityCheckedId == R.id.quantity_asc_radio) {
            quantitySort = "asc";
        } else if (quantityCheckedId == R.id.quantity_desc_radio) {
            quantitySort = "desc";
        }
        resultIntent.putExtra("quantitySort", quantitySort);

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}