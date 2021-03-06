/**
 * Copyright (c) 2016 Samsung Electronics, Inc.,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
"use strict";

monitorApp.controller("WeeklyProjectCtrl", function($scope, $http, $log, $location, $anchorScroll) {

    const columnDefs = [
        {field:'year',              displayName:'Year',         width: '5%',    headerTooltip: 'Year'},
        {field:'week',              displayName:'Week',         width: '5%',    headerTooltip: 'Week'},
        {field:'groupName',         displayName:'Group',        width: '12%',   headerTooltip: 'Group'},
        {field:'projectName',       displayName:'Project',      width: '12%',   headerTooltip: 'Project',
            cellTemplate:'<div class="ui-grid-cell-contents grid-align" uib-tooltip="Click if you want to see \'{{COL_FIELD}}\' project chart"' +
            ' tooltip-placement="top" tooltip-append-to-body="true"><a href ng-click="grid.appScope.drawChart(row.entity.projectName)">{{COL_FIELD}}</a></div>'},
        {field:'language',          displayName:'Lang',         width: '5%',    headerTooltip: 'Language'},
        {field:'allDefectCount',    displayName:'All',          width: '7%',    headerTooltip: 'All defects'},
        {field:'allNew',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[New] All types of defects" tooltip-append-to-body="true">All<br>New</div>'},
        {field:'allFix',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Fixed] All types of defects" tooltip-append-to-body="true">All<br>Fix</div>'},
        {field:'allDis',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Dismissed] All types of defects" tooltip-append-to-body="true">All<br>Dis</div>'},
        {field:'criNew',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[New] Critical defects" tooltip-append-to-body="true">Cri<br>New</div>'},
        {field:'criFix',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Fixed] Critical defects" tooltip-append-to-body="true">Cri<br>Fix</div>'},
        {field:'criDis',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Dismissed] Critical defects" tooltip-append-to-body="true">Cri<br>Dis</div>'},
        {field:'majNew',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[New] Major defects" tooltip-append-to-body="true">Maj<br>New</div>'},
        {field:'majFix',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Fixed] Major defects" tooltip-append-to-body="true">Maj<br>Fix</div>'},
        {field:'majDis',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Dismissed] Major defects" tooltip-append-to-body="true">Maj<br>Dis</div>'},
        {field:'minNew',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[New] Minor defects" tooltip-append-to-body="true">Min<br>New</div>'},
        {field:'minFix',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Fixed] Minor defects" tooltip-append-to-body="true">Min<br>Fix</div>'},
        {field:'minDis',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Dismissed] Minor defects" tooltip-append-to-body="true">Min<br>Dis</div>'},
        {field:'crcNew',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[New] Coding rule checker defects" tooltip-append-to-body="true">Crc<br>New</div>'},
        {field:'crcFix',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Fixed] Coding rule checker defects" tooltip-append-to-body="true">Crc<br>Fix</div>'},
        {field:'crcDis',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Dismissed] Coding rule checker defects" tooltip-append-to-body="true">Crc<br>Dis</div>'},
        {field:'etcNew',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[New] Etc defects" tooltip-append-to-body="true">Etc<br>New</div>'},
        {field:'etcFix',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Fixed] Etc defects" tooltip-append-to-body="true">Etc<br>Fix</div>'},
        {field:'etcDis',            width: '3%',    headerCellTemplate:'<div style="text-align: center" uib-tooltip="[Dismissed] Etc defects" tooltip-append-to-body="true">Etc<br>Dis</div>'}
    ];

    initialize();

    function initialize() {
        $scope.gridOptions = createGrid(columnDefs);
        loadDefectList();
        setGridExportingFileNames($scope.gridOptions, DEFECT_FILENAME_PREFIX);
    }

    function loadDefectList() {
        $http.get('/api/v2/defect')
            .then((res) => {
                if (!isHttpResultOK(res)) {
                    $log.error('Failed to load defect list');
                    return;
                }
                $scope.gridOptions.data = res.data.rows;
                $scope.drawChart();
            })
            .catch((err) => {
                $log.error(err);
            });
    }

    let chart;

    $scope.drawChart = function(projectName) {
        const gridData = $scope.gridOptions.data;
        if (!gridData || gridData.length <= 0) {
            return;
        }

        destroyChart();

        if (!projectName) {
            projectName = gridData[0].projectName;
        } else {
            scrollTop();
        }

        const projectData = _.filter(gridData, data => data.projectName == projectName);
        chart = new Chart($("#weekly-project"), {
            type: 'line',
            data: {
                labels: _.map(projectData, row => '' + row.year + '-' + row.week).reverse(),
                datasets: [{
                    label: 'All defects',
                    borderColor: 'red',
                    backgroundColor: 'red',
                    fill: false,
                    data: _.map(projectData, 'allDefectCount').reverse(),
                    spanGaps: false
                },{
                    label: 'Fixed defects',
                    borderColor: 'blue',
                    backgroundColor: 'blue',
                    fill: false,
                    data: _.map(projectData, 'allFix').reverse(),
                    spanGaps: false
                },{
                    label: 'Dismissed defects',
                    borderColor: 'green',
                    backgroundColor: 'green',
                    fill: false,
                    data: _.map(projectData, 'allDis').reverse(),
                    spanGaps: false
                }]
            },
            options: {
                title: {
                    display: true,
                    text: `'${projectName}' project chart`
                },
                legend: {
                    display: true
                }
            }
        });
    };

    function scrollTop() {
        $location.hash('top');
        $anchorScroll();
    }

    function destroyChart() {
        if(chart) {
            chart.destroy();
        }
    }
});