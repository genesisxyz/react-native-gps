import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { Geofence, GeofenceResult, Location } from 'react-native-gps';
import { ActivityRecognitionType, GeofenceTransition } from 'react-native-gps';

export type GeofenceColored = Geofence & { color: string };

export interface GpsState {
  lastLocation: Location | null;
  locations: Location[];
  geofenceMarkers: GeofenceColored[];
  currentActivity: string;
}

const initialState: GpsState = {
  lastLocation: null,
  locations: [],
  geofenceMarkers: [],
  currentActivity: 'Unknown',
};

export const gpsSlice = createSlice({
  name: 'gps',
  initialState,
  reducers: {
    addLocations(state, action: PayloadAction<Location[]>) {
      const { payload } = action;
      state.lastLocation = payload[payload.length - 1];
      state.locations.push(...payload);
    },
    clearLocations(state) {
      state.lastLocation = null;
      state.locations = [];
    },
    addGeofenceMarker(state, action: PayloadAction<GeofenceColored>) {
      const index = state.geofenceMarkers.findIndex((geofenceMarker) => {
        return geofenceMarker.id === action.payload.id;
      });
      if (index > -1) {
        state.geofenceMarkers[index] = action.payload;
      } else {
        state.geofenceMarkers.push(action.payload);
      }
    },
    setGeofenceTransition(state, action: PayloadAction<GeofenceResult>) {
      const { ids, transition } = action.payload;
      state.geofenceMarkers.forEach((geofenceMarker) => {
        if (ids.includes(geofenceMarker.id)) {
          geofenceMarker.color =
            transition === GeofenceTransition.Enter ? '#00ff0016' : '#ff000016';
        }
      });
    },
    clearGeofenceMarkers(state) {
      state.geofenceMarkers = [];
    },
    setCurrentActivity(state, action: PayloadAction<ActivityRecognitionType>) {
      state.currentActivity = (
        {
          [ActivityRecognitionType.InVechicle]: 'Vehicle',
          [ActivityRecognitionType.OnBicycle]: 'Bicycle',
          [ActivityRecognitionType.OnFoot]: 'Foot',
          [ActivityRecognitionType.Running]: 'Running',
          [ActivityRecognitionType.Still]: 'Still',
          [ActivityRecognitionType.Tilting]: 'Tilting',
          [ActivityRecognitionType.Unknown]: 'Unknown',
          [ActivityRecognitionType.Walking]: 'Walking',
        } as Record<ActivityRecognitionType, string>
      )[action.payload];
    },
  },
});
