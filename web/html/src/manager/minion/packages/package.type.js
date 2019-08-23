// @flow

export type ChangesMapObject = {
  [key: string]: PackagesObject
}

export type PackagesObject = {
  original: Package,
  value?: Package,
}

export type Package = {
  arch: string,
  name: string,
  packageStateId: OptionalValue,
  versionConstraintId: OptionalValue,
  epoch?: string,
  release?: string,
  version?: string,
}

export type OptionalValue = {
  value?: number,
}
