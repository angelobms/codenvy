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

import {NavbarTeamsController} from './navbar-teams/navbar-teams.controller';
import {NavbarTeams} from './navbar-teams/navbar-teams.directive';

import {CreateTeamController} from './create-team/create-team.controller';

export class TeamsConfig {

  constructor(register) {
    register.controller('NavbarTeamsController', NavbarTeamsController);
    register.directive('navbarTeams', NavbarTeams);
    register.controller('CreateTeamController', CreateTeamController);

    // config routes
    register.app.config(function ($routeProvider) {
      $routeProvider.accessWhen('/team/create', {
        title: 'New Team',
        templateUrl: 'app/teams/create-team/create-team.html',
        controller: 'CreateTeamController',
        controllerAs: 'createTeamController'
      });

    });
  }
}
