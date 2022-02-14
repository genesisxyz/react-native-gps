import {
  AppRegistry,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';

export enum LocationPermissions {
  NotDetermined = 0, // iOS only
  Restricted, // iOS only
  Denied,
  Authorized,
  AuthorizedWhenInUse, // iOS only
}

export enum ActivityPermissions {
  NotDetermined = 0, // iOS only
  Restricted, // iOS only
  Denied,
  Authorized,
}

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
  time: number;
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
    location: {
      priority: AndroidOptionsLocationPriority;
    };
  };
  ios: {
    location: {
      priority: IosOptionsLocationPriority;
    };
  };
}>;

export enum AndroidOptionsLocationPriority {
  HighAccuracy = 100,
  BalancedPowerAccuracy = 102,
  LowPower = 104,
  NoPower = 105,
}

export enum IosOptionsLocationPriority {
  AccuracyBestForNavigation = -2,
  AccuracyBest = -1,
  AccuracyNearestTenMeters = 10,
  AccuracyHundredMeters = 100,
  AccuracyKilometer = 1000,
  AccuracyThreeKilometers = 3000,
}

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
  watchLocationPermissions(
    callback: (status: LocationPermissions) => Promise<void>
  ) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask(
        'LocationPermission',
        () => async (data: { granted: boolean }) => {
          const { granted } = data;
          await callback(
            granted
              ? LocationPermissions.Authorized
              : LocationPermissions.Denied
          );
        }
      );
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchLocationPermissions', callback);
    }
  },
  watchActivityPermissions(
    callback: (status: ActivityPermissions) => Promise<void>
  ) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask(
        'ActivityPermission',
        () => async (data: { granted: boolean }) => {
          const { granted } = data;
          await callback(
            granted
              ? ActivityPermissions.Authorized
              : ActivityPermissions.Denied
          );
        }
      );
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchActivityPermissions', callback);
    }
  },
  watchLocation(callback: (location: Location[]) => void) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask(
        'Location',
        () => async (location) => callback([location])
      );
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchLocation', callback);
    }
  },
  watchGeofences(callback: (geofenceResult: GeofenceResult) => void) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask(
        'Geofence',
        () => async (geofenceResult) => callback(geofenceResult)
      );
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchGeofence', callback);
    }
  },
  watchActivity(callback: (activities: ActivityRecognition[]) => void) {
    if (Platform.OS === 'android') {
      AppRegistry.registerHeadlessTask(
        'ActivityRecognition',
        () => async (activity) => callback([activity])
      );
    } else if (Platform.OS === 'ios') {
      const myModuleEvt = new NativeEventEmitter(NativeModules.MyEventEmitter);
      myModuleEvt.addListener('watchActivity', callback);
    }
  },
};
