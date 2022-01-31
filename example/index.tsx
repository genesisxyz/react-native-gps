import React from 'react';
import { AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';
import { Provider } from 'react-redux';
import { persistor, store } from './src/store';
import { PersistGate } from 'redux-persist/integration/react';
import Gps from 'react-native-gps';
import { gpsSlice } from './src/slices/gps';

Gps.setOptions({
  android: {
    notification: {
      smallIcon: 'ic_notification',
    },
  },
});

const Redux = () => {
  return (
    <Provider store={store}>
      <PersistGate loading={null} persistor={persistor}>
        <App />
      </PersistGate>
    </Provider>
  );
};

AppRegistry.registerComponent(appName, () => Redux);

Gps.watchActivityPermissions(async () => {
  console.debug('Activity permissions granted');
});

Gps.watchLocationPermissions(async () => {
  console.debug('Location permissions granted');
});

Gps.watchLocation(async (locations) => {
  if (locations.length > 0) {
    const { latitude, longitude } = locations[locations.length - 1];
    Gps.setOptions({
      android: {
        notification: {
          contentText: `${latitude};${longitude}`,
        },
      },
    });
  }
  store.dispatch(gpsSlice.actions.addLocations(locations));
});
Gps.watchGeofences(async (geofenceResult) => {
  console.warn(geofenceResult);
  store.dispatch(gpsSlice.actions.setGeofenceTransition(geofenceResult));
});
Gps.watchActivity(async (activities) => {
  if (activities.length > 0) {
    const lastActivity = activities[activities.length - 1];
    store.dispatch(gpsSlice.actions.setCurrentActivity(lastActivity.type));
  }
});
