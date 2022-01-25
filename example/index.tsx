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

Gps.watchLocation(async (newLocation) => {
  Gps.setOptions({
    android: {
      notification: {
        contentText: `${newLocation.latitude};${newLocation.longitude}`,
      },
    },
  });
  store.dispatch(gpsSlice.actions.addLocation(newLocation));
});
Gps.watchGeofences(async (geofenceResult) => {
  console.warn(geofenceResult);
  store.dispatch(gpsSlice.actions.setGeofenceTransition(geofenceResult));
});
Gps.watchActivity(async (activity) => {
  store.dispatch(gpsSlice.actions.setCurrentActivity(activity.type));
});
