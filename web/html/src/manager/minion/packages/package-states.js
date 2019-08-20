// @flow
import React, {useEffect, useState, useRef} from "react";
import Buttons from "components/buttons";
import {InnerPanel} from 'components/panels/InnerPanel';
import {Select} from 'components/input/Select';
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
      console.log("Old PackageStateId:");
      console.log(original.packageStateId);
      console.log("New PackageStateId:");
      console.log(newPackageStateId);
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
      const currentState: InstalledPackagesObject = changed.get(key);
      const currentPackageStateId: OptionalValue = currentState !== undefined ? currentState.value.packageStateId : original.packageStateId;
      console.log("Old Constrait:");
      console.log(original.versionConstraintId);
      console.log("New Constrait:");
      console.log(newPackageConstraintId);
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
    console.log("Old Object:");
    console.log(changed.get(key));
    console.log("New Object:");
    console.log(newChanged.get(key));
    setChanged(newChanged);
  }

  // Use this one only for Select's:
  // https://github.com/Semantic-Org/Semantic-UI-React/issues/638#issuecomment-252035750
  const setUiView = () => {
    return (event, data) => {
      setView(data);
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
    <AsyncButton id="apply" action={applyPackageState} text={t("Apply changes")} disabled={changed.size > 0}/>
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

  const renderState = (row, currentState) => {
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

    // TODO: Just disable the button
    if (changed.get(packageHelpers.packageStateKey(currentState)) !== undefined) {
      undoButton = <button id={currentState.name + "-undo"} className="btn btn-default"
                           onClick={handleUndo(row.original)}>{t("Undo")}</button>
    }

    return (
      <div className="row">
        <div className={"col-md-3"}>
          <select key={currentState.name} id={currentState.name + "-pkg-state"} className="form-control"
                  value={packageHelpers.packageState2selectValue(currentState.packageStateId)}
                  onChange={handleStateChangeEvent(row.original)}>
            <option value="-1">{t("Unmanaged")}</option>
            <option value="0">{t("Installed")}</option>
            <option value="1">{t("Removed")}</option>
          </select>
        </div>
        <div className={"col-md-3"}>
          {versionConstraintSelect}
        </div>
        <div className={"col-md-3"}>
          {undoButton}
        </div>
      </div>
      )
  };

  const tableBody = () => {
    const elements = [];
    for (const row of tableRows) {
      const changed = row.value;
      const currentState = changed === undefined ? row.original : changed;

      elements.push(
        <tr key={currentState.name} id={currentState.name + "-row"} className={changed !== undefined ? "warning" : ""}>
          <td key={currentState.name}>{t(currentState.name)}</td>
          <td>
            {renderState(row, currentState)}
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

  const dropdownOptions: Array<{ key: string, value: string, show: string }> =
    [
      {key: "system", value: "system", show: "System"},
      {key: "changes", value: "changes", show: "Changes"},
      {key: "search", value: "search", show: "Search"}
    ];

  const renderSearchBar = () => {
    return (
      <div className="input-group">
        <TextField id="package-search" value={filter} placeholder={t("Search package")}
                   onChange={onSearchChange} onPressEnter={triggerSearch} className="form-control"/>
        <span className="input-group-btn">
                    <AsyncButton id="search" text={t("Search")} action={search}
                                 ref={searchRef}/>
                </span>
      </div>
    )
  };

  const renderChanges = () => {
    if (changed.size === 1) {
      return (
        <p>{t("There is 1 unsaved change")}</p>
      )
    }
    return (
      <p>{t("There are ")}{changed.size}{t(" unsaved changes")}</p>
    )
  };

  return (
    <div>
      {messages ? <Messages items={messages}/> : null}
      <InnerPanel title={t("Package States")} icon="spacewalk-icon-package-add">
        <div className={"form-horizontal"}>
          <div className="col-md-4">
            {view === "search" ? renderSearchBar() : null}
          </div>
          <div className="col-md-3">
            {renderChanges()}
          </div>
          <div className="col-md-2 btn-group">
            <div className={"pull-right"}>
              {buttons.map(button => button)}
            </div>
          </div>
          <Select
            name={"viewSelector"}
            label={t("Choose View")}
            divClass={"col-md-2"}
            labelClass={"col-md-1"}
            defaultValue={dropdownOptions[0].key}
            onChange={setUiView()}
          >
            {dropdownOptions.map(option => <option key={option.key} value={option.value}>{t(option.show)}</option>)}
          </Select>
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

