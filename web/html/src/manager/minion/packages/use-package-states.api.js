//@flow
import Network from "utils/network";
import React, {useState} from "react";
import {Utils as MessagesUtils} from "components/messages";
import type {
  InstalledPackagesObject,
  UninstalledPackage,
  InstalledPackage,
} from "./package.type";
import * as packageHelpers from "./package-utils";

const action = {
  SAVE: "Save",
  APPLY: "Apply",
  GETSERVERPACKAGES: "GetServerPackages",
  SEARCH: "Search"
};

const usePackageStatesApi = () => {
  const [messages, setMessages] = useState("");
  const [packageStates, setPackageStates] = useState<Array<InstalledPackage>>([]);
  const [searchResults, setSearchResults] = useState<Array<InstalledPackage | UninstalledPackage>>([]);

  function fetchPackageStatesApi(apiAction: string,
                                 serverId: string,
                                 filter: string = "",
                                 toSave: Map<string, InstalledPackagesObject> =
                                   new Map<string, InstalledPackagesObject>()): Promise<any> {
    if (apiAction === action.SAVE) {
      console.log("Save posted");
      return Network.post(
        "/rhn/manager/api/states/packages/save",
        JSON.stringify({
          sid: serverId,
          packageStates: Array.from(toSave.values())
        }),
        "application/json"
      ).promise
        .then((data: any) => {
            console.log("success: " + data);
            updateAfterSave(data, toSave);
            setMessages(MessagesUtils.info(t('Package states have been saved.')));
          }
        )
    } else if (apiAction === action.APPLY) {
      console.log("Apply posted");
      return Network.post(
        "/rhn/manager/api/states/apply",
        JSON.stringify({
          id: serverId,
          type: "SERVER",
          states: ["packages"]
        }),
        "application/json"
      ).promise
        .then((data) => {
          setMessages(MessagesUtils.info(<span>{t("Applying the packages states has been ")}
            <a href={"/rhn/systems/details/history/Event.do?sid=" + serverId + "&aid=" + data}>{t("scheduled")}</a>
              </span>));
        });
    } else if (apiAction === action.GETSERVERPACKAGES) {
      console.log("Getserverpackages executed");
      return Network.get(
        "/rhn/manager/api/states/packages?sid=" + serverId
      ).promise
        .then((data: Array<InstalledPackage>) => {
          updateAfterServerGetPackages(data);
        });
    } else if (apiAction === action.SEARCH) {
      console.log("Search executed");
      return Network.get(
        "/rhn/manager/api/states/packages/match?sid=" + serverId + "&target=" + filter
      ).promise
        .then((data: Array<InstalledPackage | UninstalledPackage>) => {
          console.log(data);
          updateAfterSearch(data);
          return null;
        })
    }
    return Promise.reject();
  }

  function updateAfterSearch(serverSearchResults: Array<InstalledPackage | UninstalledPackage>): void {
    const newSearchResults = serverSearchResults.map((state) => {
      state.packageStateId = packageHelpers.normalizePackageState(state.packageStateId);
      state.versionConstraintId = packageHelpers.normalizePackageVersionConstraint(state.versionConstraintId);
      return state;
    });
    setSearchResults(newSearchResults);
  }

  function updateAfterServerGetPackages(serverPackages: Array<InstalledPackage>): void {
    const newPackageStates = serverPackages.map(state => {
      state.packageStateId = packageHelpers.normalizePackageState(state.packageStateId);
      state.versionConstraintId = packageHelpers.normalizePackageVersionConstraint(state.versionConstraintId);
      return state;
    });
    setPackageStates(newPackageStates);
  }

  function updateAfterSave(newServerPackages: Array<InstalledPackage>, changed: Map<string, InstalledPackagesObject>) {
    const newPackageStates: any = newServerPackages.map((state: InstalledPackage) => {
      state.packageStateId = packageHelpers.normalizePackageState(state.packageStateId);
      return state;
    });
    const newSearchResults =
      searchResults.map<Map<string, InstalledPackagesObject | UninstalledPackage>>((state: InstalledPackage | UninstalledPackage) => {
      const tempchanged = changed.get(packageHelpers.packageStateKey(state));
      if (tempchanged !== undefined) {
        return tempchanged.value;
      } else {
        return state;
      }
    });
    setPackageStates(newPackageStates);
    setSearchResults(newSearchResults);
  }

  return {
    messages: messages, packageStates, searchResults, fetchPackageStatesApi
  }

};

export default usePackageStatesApi;
