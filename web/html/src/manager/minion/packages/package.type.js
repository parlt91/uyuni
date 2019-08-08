// @flow

export type InstalledPackagesObject = {
  original: InstalledPackage,
  value?: InstalledPackage,
}

export type UninstalledPackage = {
  arch: string,
  name: string,
  packageStateId: OptionalValue,
  versionConstraintId: OptionalValue,
}

export type InstalledPackage = {
  arch: string,
  epoch: string,
  name: string,
  packageStateId: OptionalValue,
  release: string,
  version: string,
  versionConstraintId: OptionalValue,
}

export type OptionalValue = {
  value?: number,
}
