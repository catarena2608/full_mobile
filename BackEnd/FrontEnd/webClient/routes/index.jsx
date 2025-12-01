import Layout1 from "../src/components/layout/layout";
import Login from "../src/components/login/login";
import Register from "../src/components/regis/register";
import ProtectedRoute from "./ProtectedRoute";
import MyProfile from "../src/components/myProfile/MyProfile";
import EditProfile from "../src/components/myProfileEdit/EditProfile";
import OtherProfile from "../src/components/otherProfile/otherProfile";
import HomeContent from "../src/components/home/homeContent";
import Newsfeed from "../src/components/news/Newsfeed";
import Forget from "../src/components/ForgetPassword/Forget";
import UploadPost from "../src/components/UploadPost/UploadPost";
import NormalDetail from "../src/components/NormalDetail/NormalDetail";
import RecipeDetail from "../src/components/RecipeDetail/RecipeDetail";
export const routes = [
    { path: "/login", element: <Login /> },
    { path: "/register", element: <Register /> },
    { path:"forgetPassword", element:<Forget/>},
    {
        path: "/",
        element: (
            <ProtectedRoute>
                <Layout1 />
            </ProtectedRoute>
        ), children:[{index: true, element:<Newsfeed/>},
            {path:"/users", element:<HomeContent/>},
            {path:"/myProfile", element:<MyProfile/>},
            {path:"/profile/:id", element:<OtherProfile/>},
            {path:"/profileEdit",element:<EditProfile/>},
            {path:"/uploadPost", element:<UploadPost/>},
            { path:"/normalDetail/:postID", element:<NormalDetail/> },
            { path:"/recipeDetail/:postID", element:<RecipeDetail/> }
        ]
    },
]