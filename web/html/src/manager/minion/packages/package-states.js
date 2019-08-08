// @flow
import React, {useEffect, useState, useRef} from "react";
import Buttons from "components/buttons";
import {InnerPanel} from 'components/panels/InnerPanel';
import Fields from "components/fields";
import {Messages} from "components/messages";
import {Toggler} from "components/toggler";
import withPageWrapper from "components/general/with-page-wrapper";
import {hot} from 'react-hot-loader';
import {showErrorToastr} from "../../../components/toastr/toastr";
import usePackageStatesApi from "./use-package-states.api";
import type {
  InstalledPackagesObject,
  UninstalledPackage,
  InstalledPackage,
  OptionalValue
} from "./package.type";
import * as packageHelpers from "./package-utils";
const SpaRenderer  = require("core/spa/spa-renderer").default;
const ReactDOM = require("react-dom");

const AsyncButton = Buttons.AsyncButton;
const TextField = Fields.TextField;

const action = {
  SAVE: "Save",
  APPLY: "Apply",
  GETSERVERPACKAGES: "GetServerPackages",
  SEARCH: "Search"
};

const PackageStates = ({serverId}) => {
  const [filter, setfilter] = useState<string>("");
  const [view, setView] = useState<string>("system");
  const [tableRows, setTableRows] = useState<Array<InstalledPackagesObject>>([]);
  const [changed, setChanged] =
    useState<Map<string, InstalledPackagesObject>>(new Map<string, InstalledPackagesObject>());

  const {
    messages, fetchPackageStatesApi, packageStates, searchResults
  } = usePackageStatesApi();

  useEffect(() => {
    fetchPackageStatesApi(action.GETSERVERPACKAGES, serverId)
      .catch((error => {
        showErrorToastr(error, {autoHide: false});
      }));
  }, []);

  useEffect(() => {
    console.log("View changed to: " + view);
  }, [view]);

  useEffect(() => {
    generateTableData();
  }, [changed, packageStates, searchResults, view]);

  const handleUndo = (packageState) => {
    return () => {
      const newChanged = changed;
      newChanged.delete(packageHelpers.packageStateKey(packageState));
      setChanged(newChanged);
    }
  };

  const handleStateChangeEvent = (original) => {
    return (event) => {
      const newPackageStateId: OptionalValue = packageHelpers.selectValue2PackageState(parseInt(event.target.value));
      const newPackageConstraintId: OptionalValue =
        (newPackageStateId === packageHelpers.INSTALLED ? packageHelpers.LATEST : original.versionConstraintId);
      addChanged(
        original,
        newPackageStateId,
        newPackageConstraintId
      );
    }
  };

  const handleConstraintChangeEvent = (original) => {
    return (event) => {
      const newPackageConstraintId: OptionalValue = packageHelpers.selectValue2VersionConstraints(parseInt(event.target.value));
      const key = packageHelpers.packageStateKey(original);
      const currentState: any = changed.get(key);
      const currentPackageStateId: any = currentState !== undefined ? currentState.value.packageStateId : original.packageStateId;
      addChanged(
        original,
        currentPackageStateId,
        newPackageConstraintId
      );
    }
  };

  function addChanged(original: InstalledPackage, newPackageStateId: OptionalValue, newVersionConstraintId: OptionalValue): void {
    const key = packageHelpers.packageStateKey(original);
    const newChanged = changed;
    const currentState = newChanged.get(key);
    if (currentState !== undefined
      && newPackageStateId === currentState.original.packageStateId
      && newVersionConstraintId === currentState.original.versionConstraintId) {
      newChanged.delete(key);
    } else {
      newChanged.set(key, {
        original: original,
        value: {
          arch: original.arch,
          epoch: original.epoch,
          version: original.version,
          release: original.release,
          name: original.name,
          packageStateId: newPackageStateId,
          versionConstraintId: newVersionConstraintId
        }
      });
    }
    setChanged(newChanged);
  }

  const setUiView = (view) => {
    return () => {
      setView(view);
    }
  };

  const applyPackageState = () => {
    if (changed.size > 0) {
      const response = confirm(t("There are unsaved changes. Do you want to proceed ?"));
      if (response === false) {
        return null;
      }
    }

    fetchPackageStatesApi(action.APPLY, serverId)
      .then(data => {
        console.log("apply action queued:" + data);
      }).catch(error => {
      showErrorToastr(error, {autoHide: false});
    });
  };

  const save = () => {
    return fetchPackageStatesApi(action.SAVE, serverId, "", changed)
      .then(() => {
        setView("system");
        const newChanged = new Map<string, InstalledPackagesObject>();
        setChanged(newChanged);
      }).catch(error => {
        showErrorToastr(error, {autoHide: false});
      });
  };

  const buttons = [
    <AsyncButton id="save" action={save} text={t("Save")} disabled={changed.size === 0}/>,
    <AsyncButton id="apply" action={applyPackageState} text={t("Apply")} disabled={changed.size > 0}/>
  ];

  const search = () => {
    return fetchPackageStatesApi(action.SEARCH, serverId, filter)
      .then(() => {
        setView("search");
      })
      .catch(error => {
        showErrorToastr(error, {autoHide: false});
      });
  };

  const onSearchChange = (event) => {
    setfilter(event.target.value);
  };

  const generateTableData = () => {
    let rows: Array<InstalledPackagesObject> = [];
    if (view === "system") {
      rows = packageStates.map((state: InstalledPackage) => {
        const changedPackage = changed.get(packageHelpers.packageStateKey(state));
        if (changedPackage !== undefined) {
          return changedPackage;
        } else {
          return {
            original: state,
          };
        }
      });
    } else if (view === "search") {
      rows = searchResults.map((state: InstalledPackage | UninstalledPackage) => {
        const changedPackage = changed.get(packageHelpers.packageStateKey(state));
        if (changedPackage !== undefined) {
          return changedPackage;
        } else {
          return {
            original: state,
          };
        }
      });
    } else if (view === "changes") {
      for (const state of changed.values()) {
        rows.push(state)
      }
    }
    setTableRows(rows);
  };

  const tableBody = () => {
    const elements = [];
    for (const row of tableRows) {
      const changed = row.value;
      const currentState = changed === undefined ? row.original : changed;

      let versionConstraintSelect = null;
      let undoButton = null;

      if (currentState.packageStateId === packageHelpers.INSTALLED) {
        versionConstraintSelect =
          <select id={currentState.name + "-version-constraint"} className="form-control"
                  value={packageHelpers.versionConstraints2selectValue(currentState.versionConstraintId)}
                  onChange={handleConstraintChangeEvent(row.original)}>
            <option value="0">{t("Latest")}</option>
            <option value="1">{t("Any")}</option>
          </select>;
      }
      if (changed !== undefined) {
        undoButton = <button id={currentState.name + "-undo"} className="btn btn-default"
                             onClick={handleUndo(row.original)}>{t("Undo")}</button>
      }

      elements.push(
        <tr key={currentState.name} id={currentState.name + "-row"} className={changed !== undefined ? "warning" : ""}>
          <td key={currentState.name}>{t(currentState.name)}</td>
          <td>
            <div className="form-group">
              <select key={currentState.name} id={currentState.name + "-pkg-state"} className="form-control"
                      value={packageHelpers.packageState2selectValue(currentState.packageStateId)}
                      onChange={handleStateChangeEvent(row.original)}>
                <option value="-1">{t("Unmanaged")}</option>
                <option value="0">{t("Installed")}</option>
                <option value="1">{t("Removed")}</option>
              </select>
              {versionConstraintSelect}
              {undoButton}
            </div>
          </td>
        </tr>
      );
    }
    return (
      <tbody className="table-content">
      {elements.length > 0 ? elements :
        <tr>
          <td colSpan="2">
            <div>{t("No package states.")}</div>
          </td>
        </tr>
      }
      </tbody>
    );
  };

  const searchRef = useRef<AsyncButton>();

  const triggerSearch = () => {
    searchRef.current.trigger()
  };

  return (
    <div>
      {messages ? <Messages items={messages}/> : null}
      <InnerPanel title={t("Package States")} icon="spacewalk-icon-package-add" buttons={buttons}>
        <div className="row">
          <div className="col-md-4">
            <div className="input-group">
              <TextField id="package-search" value={filter} placeholder={t("Search package")}
                         onChange={onSearchChange} onPressEnter={triggerSearch} className="form-control"/>
              <span className="input-group-btn">
                    <AsyncButton id="search" text={t("Search")} action={search}
                                 ref={searchRef}/>
                </span>
            </div>
          </div>
          <div className="col-md-6">
          </div>
          <div className="col-md-2">
            <div className="input-group">
              <Toggler id={"system"}
                       text={t("System")}
                       handler={setUiView("system")}
                       value={view === "system"}/>
              <Toggler id={"changes"}
                       text={(changed.size > 0 ? changed.size : t("No")) + t(" Changes")}
                       handler={setUiView("changes")}
                       value={view === "changes"}/>
            </div>
          </div>
        </div>
        <div className="row">
          <table className="table table-striped">
            <thead>
            <tr>
              <th>{t("Package Name")}</th>
              <th>{t("State")}</th>
            </tr>
            </thead>
            {tableBody()}
          </table>
        </div>
      </InnerPanel>
    </div>
  );
};

export default hot(module)(withPageWrapper(PackageStates));

