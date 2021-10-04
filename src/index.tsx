import { AppRegistry, NativeModules } from 'react-native';
import { Subject, takeWhile } from 'rxjs';

export type Location = {
  latitude: number;
  longitude: number;
  speed: number;
  accuracy: number;
  altitude: number;
  bearing: number;
  time: number;
  isFromMockProvider: boolean;
};

export type GeofenceResult = {
  ids: string[];
  transition: number;
};

export type Geofence = {
  id: string;
  latitude: number;
  longitude: number;
  radius: number;
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
  startLocationService(): Promise<void>;
  stopLocationService(): Promise<void>;
  startGeofenceService(): Promise<void>;
  stopGeofenceService(): Promise<void>;
  requestPermissions(): Promise<void>;
  addGeofences(geofences: Geofence[]): Promise<void>;
  removeGeofences(geofencesIds: string[]): Promise<void>;
};

const Gps: GpsType = NativeModules.Gps;

let locationFromTask = new Subject<Location | null>();

let geofenceFromTask = new Subject<GeofenceResult[] | null>();

const LocationTask = async (location: Location) => {
  locationFromTask.next(location);
};

const GeofenceTask = async (geofences: GeofenceResult[]) => {
  geofenceFromTask.next(geofences);
  console.warn(geofences);
};

AppRegistry.registerHeadlessTask('Location', () => LocationTask);

AppRegistry.registerHeadlessTask('Geofence', () => GeofenceTask);

export default {
  setOptions: Gps.setOptions,
  async startBackgroundLocation() {
    await Gps.requestPermissions();
    await Gps.startLocationService();
  },
  async stopBackgroundLocation() {
    await Gps.stopLocationService();
    await Gps.stopGeofenceService();
    locationFromTask.next(null); // unsubscribe
  },
  async startBackgroundGeofence() {
    await Gps.requestPermissions();
    await Gps.startGeofenceService();
  },
  async stopBackgroundGeofence() {
    await Gps.stopGeofenceService();
    geofenceFromTask.next(null); // unsubscribe
  },
  watchLocation(callback: (location: Location) => void) {
    locationFromTask
      .pipe(takeWhile((data) => data !== null))
      .subscribe((data) => {
        if (data) {
          callback(data);
        }
      });
  },
  async addGeofences(geofences: Geofence[]) {
    await Gps.addGeofences(geofences);
  },
  async removeGeofences(geofencesIds: string[]) {
    await Gps.removeGeofences(geofencesIds);
  },
};
