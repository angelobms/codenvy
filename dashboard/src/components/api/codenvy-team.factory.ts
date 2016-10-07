/*
 *  [2015] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
'use strict';

/**
 * This class is handling the interactions with
 * Team management API.
 *
 * @author Ann Shumilova
 */
export class CodenvyTeam {

  teamsMap : Map<string, any> = new Map();
  teams : any = [];

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($resource) {
    this.remoteTeamAPI = $resource('/api/organization', {}, {
      getTeams: {method: 'GET', url: '/api/organization', isArray: true}
    });
  }

  /**
   *
   * @returns {any}
   */
  fetchTeams() {
    let promise = this.remoteTeamAPI.getTeams().$promise;

    let resultPromise = promise.then((teams) => {
      this.teamsMap = new Map();
      this.teams = [];

      teams.forEach((team) => {
        this.teamsMap.set(team.id, team);
        this.teams.push(team);
      });
    });

    return resultPromise;
  }

  getTeams() {
    return this.teams;
  }
}
