import { Observable, Application, Permissions } from '@nativescript/core';

export class HelloWorldModel extends Observable {
  private _text: string;
  private _steps: string[];

  constructor() {
    super();
    this._text = '';
    this._steps = [];
    this.requestPermissions();
  }

  async requestPermissions() {
    try {
      const audioPermission = await Permissions.requestPermission(android.Manifest.permission.RECORD_AUDIO);
      const modifyAudioPermission = await Permissions.requestPermission(android.Manifest.permission.MODIFY_AUDIO_SETTINGS);
      console.log('Permissions granted:', audioPermission, modifyAudioPermission);
    } catch (error) {
      console.error('Error requesting permissions:', error);
    }
  }

  get text(): string {
    return this._text;
  }

  set text(value: string) {
    if (this._text !== value) {
      this._text = value;
      this.notifyPropertyChange('text', value);
    }
  }

  get steps(): string[] {
    return this._steps;
  }

  onAdd() {
    if (this._text.trim()) {
      this._steps.push(this._text.trim());
      this.text = '';
      this.notifyPropertyChange('steps', this._steps);
    }
  }
}