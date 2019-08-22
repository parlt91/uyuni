//@flow
import type {InstalledPackage, InstalledPackagesObject, OptionalValue, UninstalledPackage} from "./package.type";

const UNMANAGED = {};
const INSTALLED: OptionalValue = {value: 0};
const REMOVED: OptionalValue = {value: 1};
const PURGED: OptionalValue = {value: 2};

const LATEST: OptionalValue = {value: 0};
const ANY: OptionalValue = {value: 1};

const emptyInstalledPackagesObject: InstalledPackagesObject = {
  original: {
    arch: "",
    epoch: "",
    name: "",
    packageStateId: {},
    release: "",
    version: "",
    versionConstraintId: {}
  },
  value: {
    arch: "",
    epoch: "",
    name: "",
    packageStateId: {},
    release: "",
    version: "",
    versionConstraintId: {}
  }
};

function selectValue2PackageState(value: number): OptionalValue {
  switch (value) {
    case -1:
      return UNMANAGED;
    case 0:
      return INSTALLED;
    case 1:
      return REMOVED;
    case 2:
      return PURGED;
    default:
      return UNMANAGED;
  }
}

function packageState2selectValue(ps: OptionalValue): number {
  return ps.value !== undefined ? ps.value : -1;
}

function versionConstraints2selectValue(vc: OptionalValue): number {
  return vc.value === undefined ? 1 : vc.value;
}

function normalizePackageState(ps: OptionalValue): OptionalValue {
  return selectValue2PackageState(packageState2selectValue(ps));
}

function normalizePackageVersionConstraint(vc: OptionalValue): OptionalValue {
  return selectValue2VersionConstraints(versionConstraints2selectValue(vc))
}

function selectValue2VersionConstraints(value: number): OptionalValue {
  switch (value) {
    case 0:
      return LATEST;
    case 1:
      return ANY;
    default:
      return LATEST;
  }
}

function packageStateKey(packageState: InstalledPackage | UninstalledPackage): string {
  const version: string = (typeof packageState.version === "string") ? packageState.version : "null";
  const epoch: string = (typeof packageState.epoch === "string") ? packageState.epoch : "null";
  const release: string = (typeof packageState.release === "string" && packageState.release) ? packageState.release : "null";
  return packageState.name + version + release + epoch + packageState.arch;
}

export {
  UNMANAGED,
  INSTALLED,
  REMOVED,
  PURGED,
  LATEST,
  ANY,
  emptyInstalledPackagesObject,
  selectValue2PackageState,
  packageState2selectValue,
  versionConstraints2selectValue,
  normalizePackageState,
  normalizePackageVersionConstraint,
  selectValue2VersionConstraints,
  packageStateKey
}
