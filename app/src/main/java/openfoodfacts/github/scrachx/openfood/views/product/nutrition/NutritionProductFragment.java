package openfoodfacts.github.scrachx.openfood.views.product.nutrition;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import openfoodfacts.github.scrachx.openfood.BuildConfig;
import openfoodfacts.github.scrachx.openfood.R;
import openfoodfacts.github.scrachx.openfood.databinding.FragmentNutritionProductBinding;
import openfoodfacts.github.scrachx.openfood.fragments.BaseFragment;
import openfoodfacts.github.scrachx.openfood.images.PhotoReceiver;
import openfoodfacts.github.scrachx.openfood.images.ProductImage;
import openfoodfacts.github.scrachx.openfood.jobs.PhotoReceiverHandler;
import openfoodfacts.github.scrachx.openfood.models.HeaderNutrimentListItem;
import openfoodfacts.github.scrachx.openfood.models.NutrientLevelItem;
import openfoodfacts.github.scrachx.openfood.models.NutrientLevels;
import openfoodfacts.github.scrachx.openfood.models.NutrimentLevel;
import openfoodfacts.github.scrachx.openfood.models.NutrimentListItem;
import openfoodfacts.github.scrachx.openfood.models.Nutriments;
import openfoodfacts.github.scrachx.openfood.models.Product;
import openfoodfacts.github.scrachx.openfood.models.SendProduct;
import openfoodfacts.github.scrachx.openfood.models.State;
import openfoodfacts.github.scrachx.openfood.network.OpenFoodAPIClient;
import openfoodfacts.github.scrachx.openfood.utils.FileUtils;
import openfoodfacts.github.scrachx.openfood.utils.LocaleHelper;
import openfoodfacts.github.scrachx.openfood.utils.ProductUtils;
import openfoodfacts.github.scrachx.openfood.utils.UnitUtils;
import openfoodfacts.github.scrachx.openfood.utils.Utils;
import openfoodfacts.github.scrachx.openfood.views.AddProductActivity;
import openfoodfacts.github.scrachx.openfood.views.FullScreenActivityOpener;
import openfoodfacts.github.scrachx.openfood.views.ProductImageManagementActivity;
import openfoodfacts.github.scrachx.openfood.views.adapters.NutrientLevelListAdapter;
import openfoodfacts.github.scrachx.openfood.views.adapters.NutrimentsGridAdapter;
import openfoodfacts.github.scrachx.openfood.views.customtabs.CustomTabActivityHelper;
import openfoodfacts.github.scrachx.openfood.views.customtabs.CustomTabsHelper;
import openfoodfacts.github.scrachx.openfood.views.customtabs.WebViewFallback;
import openfoodfacts.github.scrachx.openfood.views.product.CalculateDetails;
import openfoodfacts.github.scrachx.openfood.views.product.ProductActivity;
import pl.aprilapps.easyphotopicker.EasyImage;

import static android.Manifest.permission.CAMERA;
import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.CARBOHYDRATES;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.CARBO_MAP;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.ENERGY;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.FAT;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.FAT_MAP;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.MINERALS_MAP;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.Nutriment;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.PROTEINS;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.PROT_MAP;
import static openfoodfacts.github.scrachx.openfood.models.Nutriments.VITAMINS_MAP;
import static openfoodfacts.github.scrachx.openfood.models.ProductImageField.NUTRITION;
import static openfoodfacts.github.scrachx.openfood.utils.Utils.MY_PERMISSIONS_REQUEST_CAMERA;
import static openfoodfacts.github.scrachx.openfood.utils.Utils.bold;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class NutritionProductFragment extends BaseFragment implements CustomTabActivityHelper.ConnectionCallback, PhotoReceiver {
    private static final int EDIT_PRODUCT_AFTER_LOGIN_REQUEST_CODE = 1;
    private PhotoReceiverHandler photoReceiverHandler;
    private String mUrlImage;
    private String barcode;
    private OpenFoodAPIClient api;
    //boolean to determine if image should be loaded or not
    private boolean isLowBatteryMode = false;
    private SendProduct mSendProduct;
    private CustomTabActivityHelper customTabActivityHelper;
    private Uri nutritionScoreUri;
    //the following booleans indicate whether the prompts are to be made visible
    private boolean showNutritionPrompt = false;
    private boolean showCategoryPrompt = false;
    //boolean to determine if nutrition data should be shown
    private boolean showNutritionData = true;
    private Product product;
    private State activityState;
    private FragmentNutritionProductBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        api = new OpenFoodAPIClient(getActivity());
        binding = FragmentNutritionProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        photoReceiverHandler = new PhotoReceiverHandler(this);
        // use VERTICAL divider
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.nutrimentsRecyclerView.getContext(), VERTICAL);
        binding.nutrimentsRecyclerView.addItemDecoration(dividerItemDecoration);
        binding.getNutriscorePrompt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_box_blue_18dp, 0, 0, 0);
        binding.newAdd.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_a_photo_black_18dp, 0, 0, 0);

        binding.nutriscoreLink.setOnClickListener(v -> nutriscoreLinkDisplay());
        binding.imageViewNutrition.setOnClickListener(this::openFullScreen);
        binding.calculateNutritionFacts.setOnClickListener(this::calculateNutritionFacts);
        binding.getNutriscorePrompt.setOnClickListener(v -> onNutriscoreButtonClick());
        binding.newAdd.setOnClickListener(v -> newNutritionImage());

        refreshView(getStateFromActivityIntent());
    }

    @Override
    public void refreshView(State state) {
        super.refreshView(state);

        final String langCode = LocaleHelper.getLanguage(getContext());

        activityState = state;
        product = state.getProduct();

        checkPrompts();

        showPrompts();

        if (!showNutritionData) {
            binding.imageViewNutrition.setVisibility(View.GONE);
            binding.addPhotoLabel.setVisibility(View.GONE);
            binding.imageGradeLayout.setVisibility(View.GONE);
            binding.calculateNutritionFacts.setVisibility(View.GONE);
            binding.nutrimentsCardView.setVisibility(View.GONE);
            binding.textNoNutritionData.setVisibility(View.VISIBLE);
        }

        List<NutrientLevelItem> levelItem = new ArrayList<>();

        SharedPreferences settingsPreference = getActivity().getSharedPreferences("prefs", 0);

        Nutriments nutriments = product.getNutriments();

        if (nutriments != null && !nutriments.contains(Nutriments.CARBON_FOOTPRINT)) {
            binding.textCarbonFootprint.setVisibility(View.GONE);
        }

        NutrientLevels nutrientLevels = product.getNutrientLevels();
        NutrimentLevel fat = null;
        NutrimentLevel saturatedFat = null;
        NutrimentLevel sugars = null;
        NutrimentLevel salt = null;

        if (nutrientLevels != null) {
            fat = nutrientLevels.getFat();
            saturatedFat = nutrientLevels.getSaturatedFat();
            sugars = nutrientLevels.getSugars();
            salt = nutrientLevels.getSalt();
        }

        if (fat == null && salt == null && saturatedFat == null && sugars == null) {
            binding.nutrientLevelsCardView.setVisibility(View.GONE);
            levelItem.add(new NutrientLevelItem("", "", "", 0));
            binding.imageGrade.setVisibility(View.GONE);
        } else {
            // prefetch the uri
            customTabActivityHelper = new CustomTabActivityHelper();
            customTabActivityHelper.setConnectionCallback(this);

            // TODO: Make it international
            nutritionScoreUri = Uri.parse("https://fr.openfoodfacts.org/score-nutritionnel-france");

            customTabActivityHelper.mayLaunchUrl(nutritionScoreUri, null, null);

            Context context = this.getContext();
            Nutriment fatNutriment = nutriments.get(Nutriments.FAT);
            if (fat != null && fatNutriment != null) {
                String fatNutrimentLevel = fat.getLocalize(context);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtFat),
                    fatNutriment.getDisplayStringFor100g(),
                    fatNutrimentLevel,
                    fat.getImageLevel()));
            }

            Nutriment saturatedFatNutriment = nutriments.get(Nutriments.SATURATED_FAT);
            if (saturatedFat != null && saturatedFatNutriment != null) {
                String saturatedFatLocalize = saturatedFat.getLocalize(context);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtSaturatedFat), saturatedFatNutriment.getDisplayStringFor100g(),
                    saturatedFatLocalize,
                    saturatedFat.getImageLevel()));
            }

            Nutriment sugarsNutriment = nutriments.get(Nutriments.SUGARS);
            if (sugars != null && sugarsNutriment != null) {
                String sugarsLocalize = sugars.getLocalize(context);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtSugars), sugarsNutriment.getDisplayStringFor100g(),
                    sugarsLocalize,
                    sugars.getImageLevel()));
            }

            Nutriment saltNutriment = nutriments.get(Nutriments.SALT);
            if (salt != null && saltNutriment != null) {
                String saltLocalize = salt.getLocalize(context);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtSalt), saltNutriment.getDisplayStringFor100g(),
                    saltLocalize,
                    salt.getImageLevel()));
            }

            drawNutritionGrade();
        }

        //checks the flags and accordingly sets the text of the prompt
        showPrompts();

        binding.listNutrientLevels.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.listNutrientLevels.setAdapter(new NutrientLevelListAdapter(getContext(), levelItem));

        binding.textNutriScoreInfo.setClickable(true);
        binding.textNutriScoreInfo.setMovementMethod(LinkMovementMethod.getInstance());
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.intent.putExtra("android.intent.extra.REFERRER", Uri.parse("android-app://" + getActivity().getPackageName()));
                CustomTabActivityHelper.openCustomTab(getActivity(), customTabsIntent, Uri.parse(getString(R.string.url_nutrient_values)), new WebViewFallback());
            }
        };
        spannableStringBuilder.append(getString(R.string.txtNutriScoreInfo));
        spannableStringBuilder.setSpan(clickableSpan, 0, spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.textNutriScoreInfo.setText(spannableStringBuilder);

        if (TextUtils.isEmpty(product.getServingSize())) {
            binding.textServingSize.setVisibility(View.GONE);
            binding.servingSizeCardView.setVisibility(View.GONE);
        } else {
            String servingSize = product.getServingSize();
            if (settingsPreference.getString("volumeUnitPreference", "l").equals("oz")) {
                servingSize = UnitUtils.getServingInOz(servingSize);
            } else if (servingSize.toLowerCase().contains("oz") && settingsPreference.getString("volumeUnitPreference", "l").equals("l")) {
                servingSize = UnitUtils.getServingInL(servingSize);
            }

            binding.textServingSize.setText(bold(getString(R.string.txtServingSize)));
            binding.textServingSize.append(" ");
            binding.textServingSize.append(servingSize);
        }

        if (Utils.isDisableImageLoad(getContext()) && Utils.getBatteryLevel(getContext())) {
            isLowBatteryMode = true;
        }

        if (getArguments() != null) {
            mSendProduct = (SendProduct) getArguments().getSerializable("sendProduct");
        }

        barcode = product.getCode();
        List<NutrimentListItem> nutrimentListItems = new ArrayList<>();

        final boolean inVolume = ProductUtils.isPerServingInLiter(product);
        binding.textNutrientTxt.setText(inVolume ? R.string.txtNutrientLevel100ml : R.string.txtNutrientLevel100g);
        if (isNotBlank(product.getServingSize())) {
            binding.textPerPortion.setText(getString(R.string.nutriment_serving_size) + " " + product.getServingSize());
        } else {
            binding.textPerPortion.setVisibility(View.GONE);
        }

        if (isNotBlank(product.getImageNutritionUrl(langCode))) {
            binding.addPhotoLabel.setVisibility(View.GONE);
            binding.newAdd.setVisibility(View.VISIBLE);

            // Load Image if isLowBatteryMode is false
            if (!isLowBatteryMode) {
                Picasso.get()
                    .load(product.getImageNutritionUrl(langCode))
                    .into(binding.imageViewNutrition);
            } else {

                binding.imageViewNutrition.setVisibility(View.GONE);
            }
            Picasso.get()
                .load(product.getImageNutritionUrl(langCode))
                .into(binding.imageViewNutrition);

            mUrlImage = product.getImageNutritionUrl(langCode);
        }

        //useful when this fragment is used in offline saving
        if (mSendProduct != null && isNotBlank(mSendProduct.getImgupload_nutrition())) {
            binding.addPhotoLabel.setVisibility(View.GONE);
            mUrlImage = mSendProduct.getImgupload_nutrition();
            Picasso.get().load(FileUtils.LOCALE_FILE_SCHEME + mUrlImage).config(Bitmap.Config.RGB_565).into(binding.imageViewNutrition);
        }

        if (nutriments == null) {
            return;
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.nutrimentsRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        binding.nutrimentsRecyclerView.setLayoutManager(mLayoutManager);

        binding.nutrimentsRecyclerView.setNestedScrollingEnabled(false);

        // Header hack
        nutrimentListItems.add(new NutrimentListItem(inVolume));

        // Energy
        Nutriment energy = nutriments.get(ENERGY);
        if (energy != null && UnitUtils.ENERGY_KCAL.equalsIgnoreCase(settingsPreference.getString("energyUnitPreference", UnitUtils.ENERGY_KCAL))) {
            nutrimentListItems.add(
                new NutrimentListItem(getString(R.string.nutrition_energy_short_name),
                    Utils.getEnergy(energy.getFor100gInUnits()),
                    Utils.getEnergy(energy.getForServingInUnits()),
                    UnitUtils.ENERGY_KCAL,
                    nutriments.getModifier(ENERGY)));
        } else if (energy != null && UnitUtils.ENERGY_KJ.equalsIgnoreCase(settingsPreference.getString("energyUnitPreference", UnitUtils.ENERGY_KCAL))) {
            nutrimentListItems.add(
                new NutrimentListItem(getString(R.string.nutrition_energy_short_name),
                    energy.getFor100gInUnits(),
                    energy.getForServingInUnits(),
                    UnitUtils.ENERGY_KJ.toLowerCase(),
                    nutriments.getModifier(ENERGY)));
        }

        // Fat
        Nutriment fat2 = nutriments.get(FAT);
        if (fat2 != null) {
            String modifier = nutriments.getModifier(FAT);
            nutrimentListItems.add(new HeaderNutrimentListItem(getString(R.string.nutrition_fat),
                fat2.getFor100gInUnits(),
                fat2.getForServingInUnits(),
                fat2.getUnit(),
                modifier == null ? "" : modifier));

            nutrimentListItems.addAll(getNutrimentItems(nutriments, FAT_MAP));
        }

        // Carbohydrates
        Nutriment carbohydrates = nutriments.get(CARBOHYDRATES);
        if (carbohydrates != null) {
            String modifier = nutriments.getModifier(CARBOHYDRATES);
            nutrimentListItems.add(new HeaderNutrimentListItem(getString(R.string.nutrition_carbohydrate),
                carbohydrates.getFor100gInUnits(),
                carbohydrates.getForServingInUnits(),
                carbohydrates.getUnit(),
                modifier == null ? "" : modifier));

            nutrimentListItems.addAll(getNutrimentItems(nutriments, CARBO_MAP));
        }

        // fiber
        nutrimentListItems.addAll(getNutrimentItems(nutriments, Collections.singletonMap(Nutriments.FIBER, R.string.nutrition_fiber)));

        // Proteins
        Nutriment proteins = nutriments.get(PROTEINS);
        if (proteins != null) {
            String modifier = nutriments.getModifier(PROTEINS);
            nutrimentListItems.add(new HeaderNutrimentListItem(getString(R.string.nutrition_proteins),
                proteins.getFor100gInUnits(),
                proteins.getForServingInUnits(),
                proteins.getUnit(),
                modifier == null ? "" : modifier));

            nutrimentListItems.addAll(getNutrimentItems(nutriments, PROT_MAP));
        }

        // salt and alcohol
        Map<String, Integer> map = new HashMap<>();
        map.put(Nutriments.SALT, R.string.nutrition_salt);
        map.put(Nutriments.SODIUM, R.string.nutrition_sodium);
        map.put(Nutriments.ALCOHOL, R.string.nutrition_alcohol);
        nutrimentListItems.addAll(getNutrimentItems(nutriments, map));

        // Vitamins
        if (nutriments.hasVitamins()) {
            nutrimentListItems.add(new HeaderNutrimentListItem(getString(R.string.nutrition_vitamins)));

            nutrimentListItems.addAll(getNutrimentItems(nutriments, VITAMINS_MAP));
        }

        // Minerals
        if (nutriments.hasMinerals()) {
            nutrimentListItems.add(new HeaderNutrimentListItem(getString(R.string.nutrition_minerals)));

            nutrimentListItems.addAll(getNutrimentItems(nutriments, MINERALS_MAP));
        }

        RecyclerView.Adapter adapter = new NutrimentsGridAdapter(nutrimentListItems);
        binding.nutrimentsRecyclerView.setAdapter(adapter);
    }

    private void drawNutritionGrade() {
        int nutritionGrade = Utils.getImageGrade(product);
        if (nutritionGrade != Utils.NO_DRAWABLE_RESOURCE) {
            binding.imageGradeLayout.setVisibility(View.VISIBLE);
            binding.imageGrade.setImageResource(nutritionGrade);
            binding.imageGrade.setOnClickListener(view1 -> {
                CustomTabsIntent customTabsIntent = CustomTabsHelper.getCustomTabsIntent(getContext(), customTabActivityHelper.getSession());
                CustomTabActivityHelper.openCustomTab(NutritionProductFragment.this.getActivity(), customTabsIntent, nutritionScoreUri, new WebViewFallback());
            });
        } else {
            binding.imageGradeLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Checks the product states_tags to determine which prompt to be shown
     */
    private void checkPrompts() {
        List<String> statesTags = product.getStatesTags();
        if (statesTags.contains("en:categories-to-be-completed")) {
            showCategoryPrompt = true;
        }
        if (product.getNoNutritionData() != null && product.getNoNutritionData().equals("on")) {
            showNutritionPrompt = false;
            showNutritionData = false;
        } else {
            if (statesTags.contains("en:nutrition-facts-to-be-completed")) {
                showNutritionPrompt = true;
            }
        }
    }

    private void showPrompts() {
        if (showNutritionPrompt || showCategoryPrompt) {
            binding.getNutriscorePrompt.setVisibility(View.VISIBLE);
            if (showNutritionPrompt && showCategoryPrompt) {
                binding.getNutriscorePrompt.setText(getString(R.string.add_nutrient_category_prompt_text));
            } else if (showNutritionPrompt) {
                binding.getNutriscorePrompt.setText(getString(R.string.add_nutrient_prompt_text));
            } else if (showCategoryPrompt) {
                binding.getNutriscorePrompt.setText(getString(R.string.add_category_prompt_text));
            }
        }
    }

    private List<NutrimentListItem> getNutrimentItems(Nutriments nutriments, Map<String, Integer> nutrimentMap) {
        List<NutrimentListItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : nutrimentMap.entrySet()) {
            Nutriment nutriment = nutriments.get(entry.getKey());
            if (nutriment != null) {
                items.add(new NutrimentListItem(getString(entry.getValue()),
                    nutriment.getFor100gInUnits(),
                    nutriment.getForServingInUnits(),
                    entry.getValue().equals(R.string.ph) ? "" : nutriment.getUnit(),
                    nutriments.getModifier(entry.getKey())));
            }
        }

        return items;
    }

    void nutriscoreLinkDisplay() {
        if (product.getNutritionGradeFr() != null) {
            CustomTabsIntent customTabsIntent = CustomTabsHelper.getCustomTabsIntent(getContext(), customTabActivityHelper.getSession());
            CustomTabActivityHelper.openCustomTab(NutritionProductFragment.this.getActivity(), customTabsIntent, nutritionScoreUri, new WebViewFallback());
        }
    }

    private void openFullScreen(View v) {
        if (mUrlImage != null) {
            FullScreenActivityOpener.openForUrl(this, product, NUTRITION, mUrlImage, binding.imageViewNutrition);
        } else {
            // take a picture
            if (ContextCompat.checkSelfPermission(getActivity(), CAMERA) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                EasyImage.openCamera(this, 0);
            }
        }
    }

    public void calculateNutritionFacts(View v) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .title(R.string.calculate_nutrition_facts)
            .customView(R.layout.dialog_calculate_calories, false)
            .dismissListener(dialogInterface -> Utils.hideKeyboard(getActivity()))
            .build();

        dialog.show();

        View view = dialog.getCustomView();
        if (view != null) {
            EditText etWeight = view.findViewById(R.id.edit_text_weight);
            Spinner spinner = view.findViewById(R.id.spinner_weight);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Button btn = (Button) dialog.findViewById(R.id.txt_calories_result);
                    btn.setOnClickListener(v1 -> {
                        if (!TextUtils.isEmpty(etWeight.getText().toString())) {

                            String spinnerValue = (String) spinner.getSelectedItem();
                            String weight = etWeight.getText().toString();
                            Product p = activityState.getProduct();
                            Intent intent = new Intent(getContext(), CalculateDetails.class);
                            intent.putExtra("sampleObject", p);
                            intent.putExtra("spinnervalue", spinnerValue);
                            intent.putExtra("weight", weight);
                            startActivity(intent);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), getResources().getString(R.string.please_enter_weight), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
    }

    public void onPhotoReturned(File photoFile) {
        ProductImage image = new ProductImage(barcode, NUTRITION, photoFile);
        image.setFilePath(photoFile.getAbsolutePath());
        api.postImg(getContext(), image, null);
        binding.addPhotoLabel.setVisibility(View.GONE);
        mUrlImage = photoFile.getAbsolutePath();

        Picasso.get()
            .load(photoFile)
            .fit()
            .into(binding.imageViewNutrition);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        photoReceiverHandler.onActivityResult(this, requestCode, resultCode, data);
        if (requestCode == EDIT_PRODUCT_AFTER_LOGIN_REQUEST_CODE && resultCode == RESULT_OK && isUserLoggedIn()) {
            startEditProduct();
        }
        if (ProductImageManagementActivity.isImageModified(requestCode, resultCode)
            && getActivity() instanceof ProductActivity) {
            ((ProductActivity) getActivity()).onRefresh();
        }
    }

    private void newNutritionImage() {
        doChooseOrTakePhotos(getString(R.string.nutrition_facts_picture));
    }

    @Override
    protected void doOnPhotosPermissionGranted() {
        newNutritionImage();
    }

    public String getNutrients() {
        return mUrlImage;
    }

    @Override
    public void onCustomTabsConnected() {
        binding.imageGrade.setClickable(true);
    }

    @Override
    public void onCustomTabsDisconnected() {
        binding.imageGrade.setClickable(false);
    }

    private void onNutriscoreButtonClick() {
        if (BuildConfig.FLAVOR.equals("off")) {
            if (isUserNotLoggedIn()) {
                startLoginToEditAnd(EDIT_PRODUCT_AFTER_LOGIN_REQUEST_CODE);
            } else {
                startEditProduct();
            }
        }
    }

    private void startEditProduct() {
        Intent intent = new Intent(getActivity(), AddProductActivity.class);
        intent.putExtra(AddProductActivity.KEY_EDIT_PRODUCT, product);
        //adds the information about the prompt when navigating the user to the edit the product
        intent.putExtra(AddProductActivity.MODIFY_CATEGORY_PROMPT, showCategoryPrompt);
        intent.putExtra(AddProductActivity.MODIFY_NUTRITION_PROMPT, showNutritionPrompt);
        startActivity(intent);
    }
}
