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
  startService(): Promise<void>;
  stopService(): Promise<void>;
  requestPermissions(): Promise<void>;
  requestLocationUpdates(): Promise<void>;
  removeLocationUpdates(): Promise<void>;
};

const Gps: GpsType = NativeModules.Gps;

let locationFromTask = new Subject<Location | null>();

const LocationTask = async (location: Location) => {
  locationFromTask.next(location);
};

AppRegistry.registerHeadlessTask('Location', () => LocationTask);

export default {
  setOptions: Gps.setOptions,
  async startBackgroundLocation() {
    await Gps.requestPermissions();
    await Gps.startService();
    await Gps.requestLocationUpdates();
  },
  async stopBackgroundLocation() {
    await Gps.removeLocationUpdates();
    await Gps.stopService();
    locationFromTask.next(null); // unsubscribe
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
};
