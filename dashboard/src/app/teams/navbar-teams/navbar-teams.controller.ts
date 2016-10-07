/*
 * Copyright (c) 2015-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

/**
 * @ngdoc controller
 * @name teams.navbar.controller:NavbarTeamsController
 * @description This class is handling the controller for the teams section in navbar
 * @author Ann Shumilova
 */
export class NavbarTeamsController {

  codenvyTeam: CodenvyTeam;

  /**
   * Default constructor
   * @ngInject for Dependency injection
   */
  constructor(codenvyTeam) {
    this.codenvyTeam = codenvyTeam;
    this.fetchTeams();
  }

  fetchTeams() {
    this.codenvyTeam.fetchTeams();
  }

  getTeams() {
    return this.codenvyTeam.getTeams();
  }

}
