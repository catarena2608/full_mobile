// import { configureStore, combineReducers } from "@reduxjs/toolkit";
// import userDataReducer from "../features/userData/userDataSlice";
// import postDataReducer from "../features/postData/postDataSlice"; // 🆕 thêm

import {
  persistStore,
  persistReducer,
  FLUSH,
  REHYDRATE,
  PAUSE,
  PERSIST,
  PURGE,
  REGISTER,
} from "redux-persist";
import storage from "redux-persist/lib/storage";

const rootReducer = combineReducers({
  userData: userDataReducer,
  postData: postDataReducer, // 🆕 thêm
});

const persistConfig = {
  key: "root",
  storage,
  whitelist: ["userData"], // 🧠 chỉ lưu userData thôi, KHÔNG lưu postData
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
});

export const persistor = persistStore(store);
