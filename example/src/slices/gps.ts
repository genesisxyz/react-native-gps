import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { Location } from 'react-native-gps';

export interface GpsState {
  lastLocation: Location | null;
  locations: Location[];
}

const initialState: GpsState = {
  lastLocation: null,
  locations: [],
};

export const gpsSlice = createSlice({
  name: 'gps',
  initialState,
  reducers: {
    addLocation(state, action: PayloadAction<Location>) {
      const { payload } = action;
      state.lastLocation = payload;
      state.locations.push(payload);
    },
    clearLocations(state) {
      state.lastLocation = null;
      state.locations = [];
    },
  },
});
