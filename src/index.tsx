import {
  AppRegistry,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';

export type Location = {
  latitude: number;
  longitude: number;
  speed: number | null;
  accuracy: number | null;
  altitude: number | null;
  bearing: number | null;
  time: number;
  isFromMockProvider?: boolean;
};

export enum GeofenceTransition {
  Enter = 1,
  Exit,
  Dwell,
}

export type GeofenceResult = {
  ids: string[];
  transition: GeofenceTransition;
};

export type Geofence = {
  id: string;
  latitude: number;
  longitude: number;
  radius: number;
};

export enum ActivityRecognitionType {
  InVechicle = 0,
  OnBicycle,
  OnFoot,
  Still,
  Unknown,
  Tilting,
  Walking,
  Running,
}

export type ActivityRecognition = {
  type: ActivityRecognitionType;
  confidence: number;
};

type DeepPartial<T> = {
  [P in keyof T]?: DeepPartial<T[P]>;
};

export type Options = DeepPartial<{
  android: {
    notification: {
      id: number;
      contentTitle: string;
      contentText: string;
      channel: {
        id: string;
        name: string;
        description: string;
      };
      smallIcon: string;
    };
  };
}>;

export type Prediction = {
  attributedFullText: string;
  attributedPrimaryText: string;
  attributedSecondaryText: string;
  placeID: string;
  types: any;
};

export type Place = {
  name: string;
  placeID: string;
  formattedAddress: string;
  coordinate: {
    latitude: number;
    longitude: number;
  };
};

type GpsType = {
  setOptions(options: Options): void;
  startGpsService(): Promise<boolean>;
  stopGpsService(): Promise<void>;
  startLocationUpdates(): void;
  startGeofenceUpdates(): void;
  startActivityRecognitionUpdates(): void;
  stopLocationUpdates(): void;
  stopGeofenceUpdates(): void;
  stopActivityRecognitionUpdates(): void;
  requestLocationPermissions(): Promise<boolean>;
  requestActivityPermissions(): Promise<boolean>;
  lastLocation(): Promise<Location | null>;
  addGeofences(geofences: Geofence[]): Promise<boolean>;
  removeGeofences(geofencesIds: string[]): Promise<boolean>;
  startGooglePlacesAutocompleteSession(): void;
  findAutocompletePredictions(
    query: string,
    options?: any
  ): Promise<Prediction[]>;
  getPredictionByPlaceId(placeId: string): Promise<Place | null>;
};

const Gps: GpsType = NativeModules.Gps;

export default {
  setOptions: Gps.setOptions,
  requestLocationPermissions: Gps.requestLocationPermissions,
  requestActivityPermissions: Gps.requestActivityPermissions,
  startLocationUpdates: Gps.startLocationUpdates,
  startGeofenceUpdates: Gps.startGeofenceUpdates,
  startActivityRecognitionUpdates: Gps.startActivityRecognitionUpdates,
  stopLocationUpdates: Gps.stopLocationUpdates,
  stopGeofenceUpdates: Gps.stopGeofenceUpdates,
  stopActivityRecognitionUpdates: Gps.stopActivityRecognitionUpdates,
  addGeofences: Gps.addGeofences,
  removeGeofences: Gps.removeGeofences,
  lastLocation: Gps.lastLocation,
  startGooglePlacesAutocompleteSession:
    Gps.startGooglePlacesAutocompleteSession,
  findAutocompletePredictions: Gps.findAutocompletePredictions,
  getPredictionByPlaceId: Gps.getPredictionByPlaceId,
  async startGpsService() {
    return await Gps.startGpsService();
  },
  async stopGpsService() {
    await Gps.stopGpsService();
  },
  watchLocation(callback: (location: Location) => Promise<void>) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask('Location', () => callback);
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchLocation', callback);
    }
  },
  watchGeofences(callback: (geofenceResult: GeofenceResult) => Promise<void>) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask('Geofence', () => callback);
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchGeofence', callback);
    }
  },
  watchActivity(callback: (activity: ActivityRecognition) => Promise<void>) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask('ActivityRecognition', () => callback);
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchActivity', callback);
    }
  },
};
