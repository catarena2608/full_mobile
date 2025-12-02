package course.examples.nt118.network;

import java.util.List;
import java.util.Map;

import course.examples.nt118.model.CommentListResponse;
import course.examples.nt118.model.LoginResponse;
import course.examples.nt118.model.PostsResponse;
import course.examples.nt118.model.RecipeResponse;
import course.examples.nt118.model.UploadPostResponse;
import course.examples.nt118.model.UserResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ================= AUTHENTICATION =================
    @POST("auth/login")
    Call<LoginResponse> loginUser(@Body Map<String, String> body);

    @POST("auth/register")
    Call<ResponseBody> registerUser(@Body Map<String, Object> body);

    @POST("auth/logout")
    Call<ResponseBody> logout();

    @POST("auth/forgot-password")
    Call<ResponseBody> forgotPassword(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<ResponseBody> verifyOtp(@Body Map<String, String> body);

    @POST("auth/reset-password")
    Call<ResponseBody> resetPassword(@Body Map<String, String> body);


    // ================= POSTS (Bài viết) =================
    // Lấy danh sách bài viết (Newfeed)
    @GET("post")
    Call<PostsResponse> getAllPosts(
            @Query("userID") String userID,
            @Query("after") String after,
            @Query("type") String type,
            @Query("year") Integer year,
            @Query("month") Integer month
    );

    // Lấy bài viết theo UserID
    @GET("post/{userID}")
    Call<PostsResponse> getPostsByUserID(@Path("userID") String userID);

    @GET("post/search")
    Call<PostsResponse> searchPosts(@Query("q") String query, @Query("after") String after);

    @GET("post/tag")
    Call<PostsResponse> searchPostsByTag(@Query("q") String query, @Query("after") String after);

    @GET("post/saved")
    Call<PostsResponse> getSavedPosts();

    @PATCH("post/{postID}")
    Call<ResponseBody> editPost(@Path("postID") String postID, @Body Map<String, Object> body);

    @Multipart
    @POST("post/upload")
    Call<UploadPostResponse> uploadPost(
            @Part List<MultipartBody.Part> files,
            @Part("type") RequestBody type,
            @Part("caption") RequestBody caption,
            @Part("tag") RequestBody tag,
            @Part("location") RequestBody location
    );


    // ================= INTERACTIONS (Like & Save) =================
    @POST("save")
    Call<ResponseBody> savePost(@Body Map<String, String> body);

    @HTTP(method = "DELETE", path = "save", hasBody = true)
    Call<ResponseBody> unsavePost(@Body Map<String, String> body);

    @POST("like")
    Call<ResponseBody> likePost(@Body Map<String, String> body);

    @HTTP(method = "DELETE", path = "like", hasBody = true)
    Call<ResponseBody> unlikePost(@Body Map<String, String> body);


    // ================= COMMENTS =================
    @GET("comment/{postID}")
    Call<CommentListResponse> getComments(@Path("postID") String postID, @Query("after") String after);

    @POST("comment")
    Call<ResponseBody> addComment(@Body Map<String, Object> body);

    @DELETE("comment/{id}")
    Call<ResponseBody> deleteComment(@Path("id") String commentID);


    // ================= USER & PROFILE =================
    @GET("users")
    Call<ResponseBody> getAllUsers(@Query("userID") String userID);

    @GET("users/{id}")
    Call<UserResponse> getUserById(@Path("id") String id, @Query("userID") String currentUserID);

    @GET("users/search")
    Call<ResponseBody> searchUsers(@Query("q") String query, @Query("field") String field);

    @GET("users/tag")
    Call<ResponseBody> searchUsersByTag(@Query("q") String query, @Query("after") String after);

    @PATCH("users/profile")
    Call<UserResponse> editProfile(@Body Map<String, Object> body);


    // ================= FOLLOW SYSTEM =================
    @POST("follow/{targetId}")
    Call<ResponseBody> followUser(@Path("targetId") String targetId);

    @DELETE("follow/{targetId}")
    Call<ResponseBody> unfollowUser(@Path("targetId") String targetId);


    // ================= RECIPES (Công thức) =================
    @GET("recipe/{postID}")
    Call<RecipeResponse> getRecipeByPostID(@Path("postID") String postID);

    @Multipart
    @POST("recipe/upload")
        // [NÊN SỬA] Đổi ResponseBody thành UploadPostResponse (hoặc tạo class UploadRecipeResponse tương tự)
        // để dễ dàng check response.body().isSuccess() trong Activity
    Call<UploadPostResponse> uploadRecipe(
            @Part("caption") RequestBody caption,
            @Part("postID") RequestBody postID,
            @Part("name") RequestBody name,
            @Part("description") RequestBody description,
            @Part("ration") RequestBody ration,
            @Part("time") RequestBody time,
            @Part("ingredients") RequestBody ingredients,
            @Part("guide") RequestBody guide,
            @Part("tags") RequestBody tags,
            @Part List<MultipartBody.Part> files
    );
}