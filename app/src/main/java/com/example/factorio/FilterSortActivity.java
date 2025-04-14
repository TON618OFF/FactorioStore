package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * FilterSortActivity - активность для настройки фильтров и сортировки списка товаров.
 *
 * Основные функции:
 * - Предоставление пользователю интерфейса для выбора фильтров и сортировки.
 * - Передача настроек фильтрации и сортировки обратно в вызывающую активность.
 *
 * Поля:
 * - CheckBox inStockCheckbox: Фильтр "В наличии".
 * - RadioGroup priceSortGroup: Группа переключателей для сортировки по цене.
 * - RadioGroup quantitySortGroup: Группа переключателей для сортировки по количеству.
 * - RadioGroup ratingFilterGroup: Группа переключателей для фильтрации по рейтингу.
 * - MaterialButton applyButton: Кнопка для применения настроек.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация элементов интерфейса и установка начальных значений на основе Intent.
 * - applyFiltersAndSort(): Считывает настройки фильтров и сортировки, передает их в Intent и завершает активность.
 *
 * Логика:
 * - Пользователь может выбрать:
 *   - Фильтр "В наличии".
 *   - Сортировку по цене (по возрастанию, по убыванию, без сортировки).
 *   - Сортировку по количеству (по возрастанию, по убыванию, без сортировки).
 *   - Фильтрацию по рейтингу (рейтинг 4.0, 3.0 или без фильтра).
 * - Настройки передаются в вызывающую активность через Intent с помощью метода setResult().
 */

public class FilterSortActivity extends AppCompatActivity {

    private CheckBox inStockCheckbox;
    private RadioGroup priceSortGroup;
    private RadioGroup quantitySortGroup;
    private RadioGroup ratingFilterGroup; // Новый RadioGroup
    private MaterialButton applyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_sort);

        inStockCheckbox = findViewById(R.id.in_stock_checkbox);
        priceSortGroup = findViewById(R.id.price_sort_group);
        quantitySortGroup = findViewById(R.id.quantity_sort_group);
        ratingFilterGroup = findViewById(R.id.rating_filter_group); // Инициализация
        applyButton = findViewById(R.id.apply_button);

        // Получаем текущие настройки из Intent (если есть)
        Intent intent = getIntent();
        inStockCheckbox.setChecked(intent.getBooleanExtra("inStock", false));
        String priceSort = intent.getStringExtra("priceSort");
        String quantitySort = intent.getStringExtra("quantitySort");
        double ratingFilter = intent.getDoubleExtra("ratingFilter", 0.0); // Новый параметр

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

        if (ratingFilter == 4.0) {
            ratingFilterGroup.check(R.id.rating_4_radio);
        } else if (ratingFilter == 3.0) {
            ratingFilterGroup.check(R.id.rating_3_radio);
        } else {
            ratingFilterGroup.check(R.id.rating_none_radio);
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

        // Фильтр по рейтингу
        double ratingFilter = 0.0; // 0.0 означает "без фильтра"
        int ratingCheckedId = ratingFilterGroup.getCheckedRadioButtonId();
        if (ratingCheckedId == R.id.rating_4_radio) {
            ratingFilter = 4.0;
        } else if (ratingCheckedId == R.id.rating_3_radio) {
            ratingFilter = 3.0;
        }
        resultIntent.putExtra("ratingFilter", ratingFilter);

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}