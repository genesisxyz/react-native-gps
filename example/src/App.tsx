import * as React from 'react';
import { useState } from 'react';

import {
  Button,
  FlatList,
  FlatListProps,
  LayoutRectangle,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  TouchableOpacity,
  View,
} from 'react-native';
import Gps, { Prediction } from 'react-native-gps';
import MapView, {
  Circle,
  MapEvent,
  Marker,
  Polyline,
  PROVIDER_GOOGLE,
} from 'react-native-maps';
import { useSelector } from 'react-redux';
import { RootState, useAppDispatch } from './store';
import { GeofenceColored, gpsSlice } from './slices/gps';
import { appSlice } from './slices/app';

export default function App() {
  const dispatch = useAppDispatch();
  const { lastLocation, locations, geofenceMarkers, currentActivity } =
    useSelector((state: RootState) => state.gps);
  const { latitude, longitude } = lastLocation || { latitude: 0, longitude: 0 };

  const { tracking, backgroundLocationStarted } = useSelector(
    (state: RootState) => state.app
  );

  function toggleTracking() {
    dispatch(appSlice.actions.toggleTracking());
  }

  async function clearLocations() {
    dispatch(gpsSlice.actions.clearLocations());
    await Gps.removeGeofences(
      geofenceMarkers.map((geofenceMarker) => geofenceMarker.id)
    );
    dispatch(gpsSlice.actions.clearGeofenceMarkers());
  }

  async function addGeofence(event: MapEvent) {
    if (tracking && backgroundLocationStarted) {
      const { coordinate } = event.nativeEvent;
      const geofenceMarker: GeofenceColored = {
        ...coordinate,
        id: `${Date.now()}`,
        radius: 300,
        color: '#ff000016',
      };
      dispatch(gpsSlice.actions.addGeofenceMarker(geofenceMarker));
      await Gps.addGeofences([geofenceMarker]);
    }
  }

  React.useEffect(() => {
    if (tracking) {
      new Promise(async (resolve) => {
        const granted =
          (await Gps.requestLocationPermissions()) &&
          (await Gps.requestActivityPermissions());
        if (granted) {
          const started = await Gps.startGpsService();
          return resolve(started);
        }
      }).then(async (started) => {
        if (started) {
          dispatch(appSlice.actions.setBackgroundLocationStarted(true));
        }
      });
    } else {
      Gps.stopGpsService().then(() => {
        dispatch(gpsSlice.actions.clearGeofenceMarkers());
        dispatch(appSlice.actions.setBackgroundLocationStarted(false));
      });
    }
  }, [tracking, dispatch]);

  React.useEffect(() => {
    if (tracking && backgroundLocationStarted) {
      Gps.startLocationUpdates();
      Gps.startGeofenceUpdates();
      Gps.startActivityRecognitionUpdates();
    } else {
      Gps.stopLocationUpdates();
      Gps.stopGeofenceUpdates();
      Gps.stopActivityRecognitionUpdates();
    }
  }, [tracking, backgroundLocationStarted]);

  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [showPredictions, setShowPredictions] = useState(false);

  const onChangeText: TextInputProps['onChangeText'] = async (text) => {
    if (lastLocation) {
      const newPredictions = await Gps.findAutocompletePredictions(text, {
        northEastBounds: {
          latitude,
          longitude,
        },
        southWestBounds: {
          latitude,
          longitude,
        },
      });
      setShowPredictions(newPredictions.length > 0);
      setPredictions(newPredictions);
    }
  };

  const onFocus: TextInputProps['onFocus'] = () => {
    Gps.startGooglePlacesAutocompleteSession();
  };

  let [searchBarLayout, setSearchBarLayout] = useState<LayoutRectangle>({
    height: 0,
    width: 0,
    y: 0,
    x: 0,
  });

  const searchBarOnLayout: TextInputProps['onLayout'] = (event) => {
    setSearchBarLayout(event.nativeEvent.layout);
  };

  const renderSearchPrediction: FlatListProps<Prediction>['renderItem'] = ({
    item,
  }) => {
    return (
      <TouchableOpacity
        style={{
          margin: 8,
          padding: 16,
          borderBottomWidth: StyleSheet.hairlineWidth,
          borderBottomColor: '#CCC',
        }}
        onPress={async () => {
          console.warn(item);
          await getPredictionPlace(item);
        }}
      >
        <Text style={styles.predictionText}>{item.attributedFullText}</Text>
      </TouchableOpacity>
    );
  };

  async function getPredictionPlace(prediction: Prediction) {
    setShowPredictions(false);
    const place = await Gps.getPredictionByPlaceId(prediction.placeID);
    if (place) {
      dispatch(
        gpsSlice.actions.addLocation({
          latitude: place.coordinate.latitude,
          longitude: place.coordinate.longitude,
          accuracy: 0,
          altitude: 0,
          bearing: 0,
          isFromMockProvider: true,
          speed: 0,
          time: Date.now(),
        })
      );
    }
  }

  return (
    <SafeAreaView style={styles.container}>
      <TextInput
        onLayout={searchBarOnLayout}
        placeholder="Search"
        style={styles.searchPlaces}
        onChangeText={onChangeText}
        onFocus={onFocus}
      />
      <Text style={styles.activity}>{currentActivity}</Text>
      <View style={styles.buttonContainer}>
        <Button
          disabled={
            (tracking && !backgroundLocationStarted) ||
            (!tracking && backgroundLocationStarted)
          }
          title={!tracking ? 'Start tracking' : 'Stop tracking'}
          onPress={toggleTracking}
        />
        <Button title="Clear locations" onPress={clearLocations} />
      </View>
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
            fillColor={geofenceMaker.color}
            strokeColor="#f00"
          />
        ))}
      </MapView>
      {showPredictions && (
        <View
          style={[
            styles.predictionsContainer,
            { top: searchBarLayout.y + searchBarLayout.height },
          ]}
        >
          <FlatList
            style={{ flex: 1 }}
            pointerEvents="box-none"
            data={predictions}
            renderItem={renderSearchPrediction}
            keyExtractor={(prediction) => prediction.placeID}
          />
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
  },
  buttonContainer: {
    elevation: 0,
    zIndex: 0,
  },
  map: {
    flex: 1,
  },
  activity: {
    textAlign: 'center',
    fontWeight: 'bold',
    padding: 16,
  },
  searchPlaces: {
    padding: 16,
    fontSize: 16,
  },
  predictionsContainer: {
    position: 'absolute',
    elevation: 1,
    width: '100%',
    height: 200,
    zIndex: 1,
    backgroundColor: '#FFF',
  },
  predictionText: {
    fontSize: 24,
  },
});
