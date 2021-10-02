import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface AppState {
  tracking: boolean;
  backgroundLocationStarted: boolean;
}

const initialState: AppState = {
  tracking: false,
  backgroundLocationStarted: false,
};

export const appSlice = createSlice({
  name: 'app',
  initialState,
  reducers: {
    toggleTracking: (state) => {
      state.tracking = !state.tracking;
    },
    setBackgroundLocationStarted: (state, action: PayloadAction<boolean>) => {
      const { payload } = action;
      state.backgroundLocationStarted = payload;
    },
  },
});
