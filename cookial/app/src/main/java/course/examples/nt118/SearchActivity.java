package course.examples.nt118;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import course.examples.nt118.adapter.UserAdapter;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    // UI Components
    private EditText etSearch;
    private ImageView ivSearching;
    private RecyclerView rvResults;
    private TextView tvRecentTitle, tvSeeAll;

    // Tabs UI
    private TextView tvTabUser, tvTabPost, tvTabTag;
    private View lineTabUser, lineTabPost, lineTabTag;

    // Data
    private String currentTab = "USER"; // "USER", "POST", "TAG"
    private String myUserId; // ID của người đang đăng nhập
    private UserAdapter userAdapter;

    // ==================================================================
    // 1. LIFECYCLE LOGS
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Init SearchActivity");
        setContentView(R.layout.activity_search);

        myUserId = TokenManager.getUserId(this);

        initViews();
        initAdapters();
        setupBottomNavigation();
        setupSearchInteractions();

        // Mặc định chọn tab User
        selectTab("USER");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "2. onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "3. onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "4. onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "5. onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "6. onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "7. onDestroy");
    }

    // ==================================================================
    // 2. INIT & SETUP
    // ==================================================================

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        ivSearching = findViewById(R.id.iv_searching);
        rvResults = findViewById(R.id.rv_search_history);
        tvRecentTitle = findViewById(R.id.tv_recent);
        tvSeeAll = findViewById(R.id.tv_see_all);

        // Tab Refs
        tvTabUser = findViewById(R.id.tv_tab_user);
        lineTabUser = findViewById(R.id.line_tab_user);

        tvTabPost = findViewById(R.id.tv_tab_post);
        lineTabPost = findViewById(R.id.line_tab_post);

        tvTabTag = findViewById(R.id.tv_tab_tag);
        lineTabTag = findViewById(R.id.line_tab_tag);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initAdapters() {
        // [LOGIC CLICK] Chuyển hướng sang OtherUserProfileActivity
        userAdapter = new UserAdapter(this, new ArrayList<>(), false, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UserResponse user) {
                // Gọi hàm xử lý chuyển trang thông minh
                openUserProfile(user.getId());
            }

            @Override
            public void onRemoveClick(UserResponse user, int position) { }
        });

        rvResults.setAdapter(userAdapter);
    }

    /**
     * Hàm xử lý chuyển trang Profile
     * - Nếu click vào chính mình -> ProfileActivity
     * - Nếu click vào người khác -> OtherUserProfileActivity
     */
    private void openUserProfile(String targetUserId) {
        if (targetUserId == null) return;

        Intent intent;
        if (targetUserId.equals(myUserId)) {
            Log.d(TAG, "User clicked on self -> Opening ProfileActivity");
            intent = new Intent(SearchActivity.this, ProfileActivity.class);
        } else {
            Log.d(TAG, "User clicked on ID: " + targetUserId + " -> Opening OtherUserProfileActivity");
            intent = new Intent(SearchActivity.this, OtherUserProfileActivity.class);
            intent.putExtra("USER_ID", targetUserId);
        }
        startActivity(intent);
    }

    // ==================================================================
    // 3. SEARCH INTERACTIONS
    // ==================================================================

    private void setupSearchInteractions() {
        ivSearching.setOnClickListener(v -> performSearch());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Vui lòng nhập từ khóa", Toast.LENGTH_SHORT).show();
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }

        tvRecentTitle.setText("Kết quả tìm kiếm");
        tvSeeAll.setVisibility(View.GONE);

        if ("USER".equals(currentTab)) {
            callApiSearchUser(query);
        } else if ("TAG".equals(currentTab)) {
            callApiSearchTag(query);
        } else {
            Toast.makeText(this, "Tính năng tìm kiếm bài viết đang phát triển", Toast.LENGTH_SHORT).show();
            userAdapter.setData(new ArrayList<>());
        }
    }

    // ==================================================================
    // 4. API CALLS
    // ==================================================================

    private void callApiSearchUser(String query) {
        Log.d(TAG, "API: Searching User with query: " + query);
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.searchUsers(query, null).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                handleSearchResponse(response);
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Search User Failed", t);
                Toast.makeText(SearchActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callApiSearchTag(String query) {
        Log.d(TAG, "API: Searching Tag with query: " + query);
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.searchUsersByTag(query, null).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                handleSearchResponse(response);
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Search Tag Failed", t);
                Toast.makeText(SearchActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSearchResponse(Response<UserResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            List<UserResponse> users = response.body().getListUsers();

            if (users != null && !users.isEmpty()) {
                Log.d(TAG, "API Success: Found " + users.size() + " users.");
                userAdapter.setData(users);
                if (rvResults.getAdapter() != userAdapter) {
                    rvResults.setAdapter(userAdapter);
                }
            } else {
                Log.d(TAG, "API Success: No users found.");
                userAdapter.setData(new ArrayList<>());
                Toast.makeText(SearchActivity.this, "Không tìm thấy kết quả nào", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "API Error: " + response.code());
            userAdapter.setData(new ArrayList<>());
            Toast.makeText(SearchActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================================================================
    // 5. TAB LOGIC
    // ==================================================================

    public void onTabClicked(View view) {
        int id = view.getId();
        if (id == R.id.tab_user) selectTab("USER");
        else if (id == R.id.tab_post) selectTab("POST");
        else if (id == R.id.tab_tag) selectTab("TAG");
    }

    private void selectTab(String tabName) {
        Log.d(TAG, "Tab Selected: " + tabName);
        this.currentTab = tabName;
        resetTabsUI();
        highlightTabUI(tabName);

        userAdapter.setData(new ArrayList<>());

        String query = etSearch.getText().toString().trim();
        if (!TextUtils.isEmpty(query)) {
            performSearch();
        } else {
            showHistoryUI();
        }
    }

    private void showHistoryUI() {
        tvRecentTitle.setText("Mới đây");
        tvSeeAll.setVisibility(View.VISIBLE);
    }

    private void highlightTabUI(String tabName) {
        int orange = Color.parseColor("#FF9800");
        if ("USER".equals(tabName)) setTabActive(tvTabUser, lineTabUser, orange);
        else if ("POST".equals(tabName)) setTabActive(tvTabPost, lineTabPost, orange);
        else if ("TAG".equals(tabName)) setTabActive(tvTabTag, lineTabTag, orange);
    }

    private void setTabActive(TextView tv, View line, int color) {
        if (tv != null) {
            tv.setTextColor(color);
            tv.setTypeface(null, Typeface.BOLD);
        }
        if (line != null) {
            line.setVisibility(View.VISIBLE);
            line.setBackgroundColor(color);
        }
    }

    private void resetTabsUI() {
        int gray = Color.parseColor("#757575");
        resetTabSingle(tvTabUser, lineTabUser, gray);
        resetTabSingle(tvTabPost, lineTabPost, gray);
        resetTabSingle(tvTabTag, lineTabTag, gray);
    }

    private void resetTabSingle(TextView tv, View line, int color) {
        if (tv != null) {
            tv.setTextColor(color);
            tv.setTypeface(null, Typeface.NORMAL);
        }
        if (line != null) line.setVisibility(View.INVISIBLE);
    }

    // ==================================================================
    // 6. NAVIGATION
    // ==================================================================
    private void setupBottomNavigation() {
        View navView = findViewById(R.id.bottom_navigation_bar);
        if (navView != null) {
            View navHome = navView.findViewById(R.id.nav_home);
            View navAdd = navView.findViewById(R.id.nav_add);
            View navSaved = navView.findViewById(R.id.nav_saved);
            View navProfile = navView.findViewById(R.id.nav_profile);

            if (navHome != null) navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
            if (navAdd != null) navAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, NewPostActivity.class));
            });
            if (navSaved != null) navSaved.setOnClickListener(v -> {
                startActivity(new Intent(this, SavedPostsActivity.class));
            });
            if (navProfile != null) navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("USER_ID", myUserId);
                startActivity(intent);
            });
        }
    }
}