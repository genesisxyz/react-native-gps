import * as React from 'react';

import { StyleSheet, View, Button, Text } from 'react-native';
import Gps, { ActivityRecognitionType, Geofence } from 'react-native-gps';
import MapView, {
  Circle,
  MapEvent,
  Marker,
  Polyline,
  PROVIDER_GOOGLE,
} from 'react-native-maps';
import { useSelector } from 'react-redux';
import type { RootState } from './store';
import { useAppDispatch } from './store';
import { gpsSlice } from './slices/gps';
import { appSlice } from './slices/app';
import { useState } from 'react';

const activities = {
  [ActivityRecognitionType.InVechicle]: 'Vehicle',
  [ActivityRecognitionType.OnBicycle]: 'Bicycle',
  [ActivityRecognitionType.OnFoot]: 'Foot',
  [ActivityRecognitionType.Running]: 'Running',
  [ActivityRecognitionType.Still]: 'Still',
  [ActivityRecognitionType.Tilting]: 'Tilting',
  [ActivityRecognitionType.Unknown]: 'Unknown',
  [ActivityRecognitionType.Walking]: 'Walking',
};

export default function App() {
  const dispatch = useAppDispatch();
  const { lastLocation, locations } = useSelector(
    (state: RootState) => state.gps
  );
  const { latitude, longitude } = lastLocation || { latitude: 0, longitude: 0 };

  const { tracking, backgroundLocationStarted } = useSelector(
    (state: RootState) => state.app
  );

  const [geofenceMarkers, setGeofenceMarkers] = useState<Geofence[]>([]);

  const [currentActivity, setCurrentActivity] = useState(
    activities[ActivityRecognitionType.Unknown]
  );

  function toggleTracking() {
    dispatch(appSlice.actions.toggleTracking());
  }

  function clearLocations() {
    dispatch(gpsSlice.actions.clearLocations());
    setGeofenceMarkers((state) => {
      Gps.removeGeofences(state.map((geofenceMarker) => geofenceMarker.id));
      return [];
    });
  }

  function addGeofence(event: MapEvent) {
    if (tracking && backgroundLocationStarted) {
      const { coordinate } = event.nativeEvent;
      const geofenceMarker: Geofence = {
        ...coordinate,
        id: `${latitude}${longitude}`,
        radius: 300,
      };
      setGeofenceMarkers((state) => [...state, geofenceMarker]);
      Gps.addGeofences([geofenceMarker]);
    }
  }

  React.useEffect(() => {
    if (tracking) {
      Gps.startBackgroundLocation().then(() => {
        Gps.startBackgroundGeofence().then(() => {
          Gps.startBackgroundActivityRecognition().then(() => {
            dispatch(appSlice.actions.setBackgroundLocationStarted(true));
          });
        });
      });
    } else {
      Gps.stopBackgroundLocation().then(() => {
        Gps.stopBackgroundGeofence().then(() => {
          Gps.stopBackgroundActivityRecognition().then(() => {
            setGeofenceMarkers([]);
            dispatch(appSlice.actions.setBackgroundLocationStarted(false));
          });
        });
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
      Gps.watchActivity((activity) => {
        setCurrentActivity(activities[activity.type]);
      });
    }
  }, [tracking, backgroundLocationStarted, dispatch]);

  return (
    <View style={styles.container}>
      <Text style={styles.activity}>{currentActivity}</Text>
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
        onPress={addGeofence}
      >
        <Marker coordinate={{ latitude, longitude }} />
        <Polyline coordinates={locations} strokeWidth={6} />
        {geofenceMarkers.map((geofenceMaker) => (
          <Circle
            key={geofenceMaker.id}
            center={{
              latitude: geofenceMaker.latitude,
              longitude: geofenceMaker.longitude,
            }}
            radius={geofenceMaker.radius}
            fillColor="#ff000016"
            strokeColor="#f00"
          />
        ))}
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
  activity: {
    textAlign: 'center',
    fontWeight: 'bold',
    padding: 16,
  },
});
