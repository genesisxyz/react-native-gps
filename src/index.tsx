import { AppRegistry, NativeModules } from 'react-native';
import { BehaviorSubject } from 'rxjs';
import type { Subscription } from 'rxjs';

export type Location = {
  latitude: number;
  longitude: number;
  speed: number;
  accuracy: number;
  altitude: number;
  bearing: number;
  time: number;
  isFromMockProvider?: boolean;
};

export enum GeofenceTransition {
  Enter = 1,
  Exit,
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
};

const Gps: GpsType = NativeModules.Gps;

let locationFromTask = new BehaviorSubject<Location | null>(null);

let geofenceFromTask = new BehaviorSubject<GeofenceResult | null>(null);

let activityRecognitionFromTask =
  new BehaviorSubject<ActivityRecognition | null>(null);

const LocationTask = async (location: Location) => {
  locationFromTask.next(location);
};

const GeofenceTask = async (geofenceResult: GeofenceResult) => {
  geofenceFromTask.next(geofenceResult);
};

const ActivityRecognitionTask = async (activity: ActivityRecognition) => {
  activityRecognitionFromTask.next(activity);
};

AppRegistry.registerHeadlessTask('Location', () => LocationTask);

AppRegistry.registerHeadlessTask('Geofence', () => GeofenceTask);

AppRegistry.registerHeadlessTask(
  'ActivityRecognition',
  () => ActivityRecognitionTask
);

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
  async startGpsService() {
    return await Gps.startGpsService();
  },
  async stopGpsService() {
    await Gps.stopGpsService();
  },
  watchLocation(callback: (location: Location) => void): Subscription {
    return locationFromTask.subscribe((data) => {
      if (data !== null) {
        callback(data);
      }
    });
  },
  watchGeofences(
    callback: (geofenceResult: GeofenceResult) => void
  ): Subscription {
    return geofenceFromTask.subscribe((data) => {
      if (data !== null) {
        callback(data);
      }
    });
  },
  watchActivity(
    callback: (activity: ActivityRecognition) => void
  ): Subscription {
    return activityRecognitionFromTask.subscribe((data) => {
      if (data !== null) {
        callback(data);
      }
    });
  },
};
