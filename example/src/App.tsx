import * as React from 'react';

import { StyleSheet, View, Button } from 'react-native';
import Gps from 'react-native-gps';
import MapView, { Marker, Polyline, PROVIDER_GOOGLE } from 'react-native-maps';
import { useSelector } from 'react-redux';
import type { RootState } from './store';
import { useAppDispatch } from './store';
import { gpsSlice } from './slices/gps';
import { appSlice } from './slices/app';

export default function App() {
  const dispatch = useAppDispatch();
  const { lastLocation, locations } = useSelector(
    (state: RootState) => state.gps
  );
  const { latitude, longitude } = lastLocation || { latitude: 0, longitude: 0 };

  const { tracking, backgroundLocationStarted } = useSelector(
    (state: RootState) => state.app
  );

  function toggleTracking() {
    dispatch(appSlice.actions.toggleTracking());
  }

  function clearLocations() {
    dispatch(gpsSlice.actions.clearLocations());
  }

  React.useEffect(() => {
    if (tracking) {
      Gps.startBackgroundLocation().then(() => {
        dispatch(appSlice.actions.setBackgroundLocationStarted(true));
      });
    } else {
      Gps.stopBackgroundLocation().then(() => {
        dispatch(appSlice.actions.setBackgroundLocationStarted(false));
      });
    }
  }, [tracking, dispatch]);

  React.useEffect(() => {
    if (tracking && backgroundLocationStarted) {
      Gps.watchLocation((newLocation) => {
        Gps.setOptions({
          android: {
            notification: {
              contentText: `${newLocation.latitude};${newLocation.longitude}`,
            },
          },
        });
        dispatch(gpsSlice.actions.addLocation(newLocation));
      });
    }
  }, [tracking, backgroundLocationStarted, dispatch]);

  return (
    <View style={styles.container}>
      <Button
        disabled={
          (tracking && !backgroundLocationStarted) ||
          (!tracking && backgroundLocationStarted)
        }
        title={!tracking ? 'Start tracking' : 'Stop tracking'}
        onPress={toggleTracking}
      />
      <Button title="Clear locations" onPress={clearLocations} />
      <MapView
        provider={PROVIDER_GOOGLE} // remove if not using Google Maps
        style={styles.map}
        region={{
          latitude,
          longitude,
          latitudeDelta: 0.015,
          longitudeDelta: 0.0121,
        }}
      >
        <Marker coordinate={{ latitude, longitude }} />
        <Polyline coordinates={locations} strokeWidth={6} />
      </MapView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
  },
  map: {
    flex: 1,
  },
});
