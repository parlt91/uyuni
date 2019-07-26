/* eslint-disable */
import React from "react";
import Buttons from "components/buttons";
import {InnerPanel} from 'components/panels/InnerPanel';
import Network from "utils/network";
import Fields from "components/fields";
import {Messages, Utils as MessagesUtils} from "components/messages";
import {Toggler} from "components/toggler";
import withPageWrapper from "components/general/with-page-wrapper";
import {hot} from 'react-hot-loader';
const SpaRenderer  = require("core/spa/spa-renderer").default;
const ReactDOM = require("react-dom");

const AsyncButton = Buttons.AsyncButton;
const TextField = Fields.TextField;

const UNMANAGED = {};
const INSTALLED = {value: 0};
const REMOVED = {value: 1};
const PURGED = {value: 2};

const LATEST = {value: 0};
const ANY = {value: 1};

function selectValue2PackageState(value) {
    switch(value){
        case -1: return UNMANAGED;
        case 0: return INSTALLED;
        case 1: return REMOVED;
        case 2: return PURGED;
    }
}

function packageState2selectValue(ps) {
    return ps.value !== undefined ? ps.value : -1;
}

function versionConstraints2selectValue(vc) {
    return vc.value;
}

function normalizePackageState(ps) {
    return selectValue2PackageState(packageState2selectValue(ps));
}

function normalizePackageVersionConstraint(vc) {
    return selectValue2VersionConstraints(versionConstraints2selectValue(vc))
}

function selectValue2VersionConstraints(value) {
    switch(value){
        case 0: return LATEST;
        case 1: return ANY;
    }
}

function packageStateKey(packageState) {
    return packageState.name + packageState.version +
           packageState.release + packageState.epoch +
           packageState.arch;
}

class PackageStates extends React.Component {

  constructor(props) {
    super(props);
    ["init", "tableBody", "handleStateChange", "onSearchChange", "search", "save", "setView", "addChanged",
    "triggerSearch", "applyPackageState"]
    .forEach(method => this[method] = this[method].bind(this));
    this.state = {
        filter: "",
        view: "system",
        packageStates: [],
        search: {
            filter: null,
            results: []
        },
        changed: new Map()
    };
    this.init();
  }

  init () {
    Network.get("/rhn/manager/api/states/packages?sid=" + this.props.serverId).promise.then(data => {
      console.log(data);
      this.setState({
        packageStates: data.map(state => {
          state.packageStateId = normalizePackageState(state.packageStateId);
          state.versionConstraintId = normalizePackageVersionConstraint(state.versionConstraintId);
          return state;
        })
      });
    });
  }

  triggerSearch() {
    this.searchButton.trigger()
  }

  search () {
    if (this.state.filter === this.state.search.filter) {
        this.setState({
            view: "search"
        });
        return Promise.resolve();
    } else {
       return Network.get("/rhn/manager/api/states/packages/match?sid=" + this.props.serverId + "&target=" + this.state.filter).promise.then(data => {
          console.log(data);
          this.setState({
            view: "search",
            search:  {
                filter: this.state.filter,
                results: data.map(state => {
                  state.packageStateId = normalizePackageState(state.packageStateId);
                  return state;
                })
            }
          });
        });
    }
  }

  save() {
    const states = [];
    for(var state of this.state.changed.values()) {
        states.push(state.value)
    }
    const request = Network.post(
        "/rhn/manager/api/states/packages/save",
        JSON.stringify({
            sid: this.props.serverId,
            packageStates: states
        }),
        "application/json"
    ).promise.then(data => {
      console.log("success: " + data);
      const newPackageStates = data.map(state => {
          state.packageStateId = normalizePackageState(state.packageStateId);
          return state;
      });

      const newSearchResults = this.state.search.results.map( state => {
        const changed = this.state.changed.get(packageStateKey(state))
        if(changed !== undefined) {
            return changed.value;
        } else{
            return state;
        }
      });

      this.setState({
        changed: new Map(),
        view: "system",
        search: {
            filter: this.state.search.filter,
            results: newSearchResults
        },
        packageStates: newPackageStates,
        messages: MessagesUtils.info(t('Package states have been saved.'))
      });
    }, jqXHR => {
      console.log("fail: " + jqXHR);
      throw "failed to save";
    });
    return request;
  }

  applyPackageState () {
    if (this.state.changed.size > 0) {
        const response = confirm(t("There are unsaved changes. Do you want to proceed ?"))
        if (response == false) {
            return null;
        }
    }

    const request = Network.post(
        "/rhn/manager/api/states/apply",
        JSON.stringify({
            id: this.props.serverId,
            type: "SERVER",
            states: ["packages"]
        }),
        "application/json"
    );
    return request.promise.then(data => {
          console.log("apply action queued:" + data);
          this.setState({
              messages: MessagesUtils.info(<span>{t("Applying the packages states has been ")}
                  <a href={"/rhn/systems/details/history/Event.do?sid=" + this.props.serverId + "&aid=" + data}>{t("scheduled")}</a>
              </span>)
          });
    });
  }

  setView(view) {
    return () => {
      this.setState({
          view: view
      });
    }
  }

  tableBody() {
    const elements = [];
    var rows = [];
    if(this.state.view === "system") {
        rows = this.state.packageStates.map(state => {
            const changed = this.state.changed.get(packageStateKey(state))
            if(changed !== undefined) {
                return changed;
            } else {
                return {
                    original: state,
                };
            }
        });
    } else if(this.state.view === "search") {
        rows = this.state.search.results.map(state => {
            const changed = this.state.changed.get(packageStateKey(state))
            if(changed !== undefined) {
                return changed;
            } else {
                return {
                    original: state,
                };
            }
        });
    } else if(this.state.view === "changes") {
        for(var state of this.state.changed.values()) {
            rows.push(state)
        }
    }
    for(var row of rows) {
      const changed = row.value;
      const currentState = changed === undefined? row.original : changed;

      var versionConstraintSelect = null;
      if(currentState.packageStateId === INSTALLED) {
        versionConstraintSelect =
            <select id={currentState.name + "-version-constraint"} className="form-control" value={versionConstraints2selectValue(currentState.versionConstraintId)} onChange={this.handleConstraintChange(row.original)}>
              <option value="0">{t("Latest")}</option>
              <option value="1">{t("Any")}</option>
            </select>;
      }
      var undoButton = null;
      if(changed !== undefined) {
        undoButton = <button id={currentState.name + "-undo"} className="btn btn-default" onClick={this.handleUndo(row.original)}>{t("Undo")}</button>
      }

      elements.push(
        <tr id={currentState.name + "-row"} className={changed !== undefined ? "warning" : ""}>
          <td>{t(currentState.name)}</td>
          <td>
            <div className="form-group">
              <select id={currentState.name + "-pkg-state"} className="form-control" value={packageState2selectValue(currentState.packageStateId)} onChange={this.handleStateChange(row.original)}>
                <option value="-1">{t("Unmanaged")}</option>
                <option value="0">{t("Installed")}</option>
                <option value="1">{t("Removed")}</option>
              </select>
              { versionConstraintSelect }
              { undoButton }
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
  }

  onSearchChange(event) {
    this.setState({
        filter: event.target.value
    });
  }

  handleUndo(packageState) {
      return event => {
         this.state.changed.delete(packageStateKey(packageState));
         this.setState({
            changed: this.state.changed
         });
      }
  }

  addChanged(original, newPackageStateId, newVersionConstraintId) {
      const key = packageStateKey(original);
      const currentState = this.state.changed.get(key);
      if (currentState != undefined &&
          newPackageStateId ==  currentState.original.packageStateId &&
          newVersionConstraintId ==  currentState.original.versionConstraintId) {
            this.state.changed.delete(key);
      } else {
            this.state.changed.set(key, {
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
      this.setState({
         changed: this.state.changed
      });
  }

  handleStateChange(original) {
      return event => {
         const newPackageStateId = selectValue2PackageState(parseInt(event.target.value));
         this.addChanged(
            original,
            newPackageStateId,
            newPackageStateId == INSTALLED ? LATEST :  original.versionConstraintId
         );
      }
  }

  handleConstraintChange(original) {
      return event => {
         const newPackageConstraintId = selectValue2VersionConstraints(parseInt(event.target.value));
         const key = packageStateKey(original);
         const currentState = this.state.changed.get(key);
         const currentPackageStateId = currentState != undefined ? currentState.value.packageStateId : original.packageStateId;
         this.addChanged(
            original,
            currentPackageStateId,
            newPackageConstraintId
         );
      }
  }


  render() {

    const messages = this.state.messages ?
          <Messages items={this.state.messages}/>
          : null;
    const buttons = [
              <AsyncButton id="save" action={this.save} text={t("Save")} disabled={this.state.changed.size == 0}/>,
              <AsyncButton id="apply" action={this.applyPackageState} text={t("Apply")} disabled={this.state.changed.size > 0}/>
    ];

    return (
      <div>
        {messages}
        <InnerPanel title={t("Package States")} icon="spacewalk-icon-package-add" buttons={buttons}>
          <div className="row">
            <div className="col-md-4">
              <div className="input-group">
                <TextField id="package-search" value={this.state.filter} placeholder={t("Search package")}
                           onChange={this.onSearchChange} onPressEnter={this.triggerSearch} className="form-control"/>
                <span className="input-group-btn">
                    <AsyncButton id="search" text={t("Search")} action={this.search}
                                 ref={(c) => this.searchButton = c}/>
                </span>
              </div>
            </div>
            <div className="col-md-6">
            </div>
            <div className="col-md-2">
              <div className="input-group">
                <Toggler id={"system"}
                         text={t("System")}
                         handler={this.setView("system")}
                         value={this.state.view == "system"}/>
                <Toggler id={"changes"}
                         text={this.state.changed.size > 0 ? this.state.changed.size : t("No") + t(" Changes")}
                         handler={this.setView("changes")}
                         value={this.state.view == "changes"}/>
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
              {this.tableBody()}
            </table>
          </div>
        </InnerPanel>
      </div>
    );
  }
}

export default hot(module)(withPageWrapper(PackageStates));

