import { createSlice } from "@reduxjs/toolkit";

const postDataSlice = createSlice({
  name: "postData",
  initialState: null, // post đang xem
  reducers: {
    setPost: (state, action) => action.payload,
    clearPost: () => null,
  },
});

export const { setPost, clearPost } = postDataSlice.actions;
export default postDataSlice.reducer;
