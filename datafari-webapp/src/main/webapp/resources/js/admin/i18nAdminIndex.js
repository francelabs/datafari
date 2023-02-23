$(function($) {

  // 'this' is the document (adminUI index page) here
  var elm = $(this);

  // Set the language parameter for search page
  elm.find('#datafariSearchUiLink').prop('href', "/");

  // Service Administration
  elm.find('#service-administration-AdminUI').html(window.i18n.msgStore['adminUI-ServiceAdministration']);
  elm.find('#service-restart-AdminUI').html(window.i18n.msgStore['adminUI-ClusterActions-ServiceRestart']);
  elm.find('#service-backup-AdminUI').html(window.i18n.msgStore['adminUI-ClusterActions-ServiceBackup']);
  elm.find('#service-reinit-AdminUI').html(window.i18n.msgStore['adminUI-ClusterActions-ServiceReinitialization']);
  elm.find('#service-reset-AdminUI').html(window.i18n.msgStore['adminUI-ClusterActions-ServiceFactoryReset']);
  elm.find('#service-restore-AdminUI').html(window.i18n.msgStore['adminUI-ClusterActions-ServiceRestore']);

  // Statistics
  elm.find('#usagesAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-UsagesAnalysis']);
  elm.find('#corpusAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-CorpusAnalysis']);
  elm.find('#corpusOTAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-CorpusOTAnalysis']);
  elm.find('#queriesAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-QueriesAnalysis']);
  elm.find('#systemAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-SystemAnalysis']);
  elm.find('#crawlDataMonitoring-AdminUI').html(window.i18n.msgStore['crawlDataMonitoring-AdminUI']);
  elm.find('#problematicFiles-AdminUI').html(window.i18n.msgStore['adminUI-ProblematicFiles']);
  elm.find('#duplicates-AdminUI').html(window.i18n.msgStore['adminUI-Duplicates']);
  elm.find('#logsAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-LogsAnalysis']);
  elm.find('#metricbeatOverview-AdminUI').html(window.i18n.msgStore['metricbeatOverview-AdminUI']);

  elm.find('#relevancySetupFile-AdminUI').html(window.i18n.msgStore['adminUI-RelevancySetupFile']);

  elm.find('#datafariSearchUiLink').html(window.i18n.msgStore['adminUI-SearchPage']);
  elm.find('#welcomeAdminUiMsg').html(window.i18n.msgStore['adminUI-Welcome']);
  elm.find('#logout-AdminUI').html(window.i18n.msgStore['logoutAdminUiLink']);
  elm.find('#myAccount-AdminUI').html(window.i18n.msgStore['adminUI-MyAccount']);
  elm.find('#alerts-AdminUI').html(window.i18n.msgStore['adminUI-Alerts']);
  elm.find('#favorites-AdminUI').html(window.i18n.msgStore['adminUI-Favorites']);
  elm.find('#searches-AdminUI').html(window.i18n.msgStore['adminUI-Searches']);
  elm.find('#connectors-AdminUI').html(window.i18n.msgStore['adminUI-Connectors']);
  elm.find('#MCFAdmin-AdminUI').html(window.i18n.msgStore['adminUI-Connectors-Admin']);
  elm.find('#MCFPassword-AdminUI').html(window.i18n.msgStore['MCFPassword-AdminUI']);
  elm.find('#MCFSimplified-AdminUI').html(window.i18n.msgStore['adminUI-Connectors-MCFSimplified']);
  // MCF distant

  elm.find('#annotatorConfiguration-AdminUI').html(window.i18n.msgStore['adminUI-AnnotatorConf']);
  elm.find('#duplicatesConfiguration-AdminUI').html(window.i18n.msgStore['adminUI-DuplicatesConf']);
  elm.find('#searchEngineAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SearchEngineAdmin']);
  elm.find('#solrAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SolrAdmin']);
  elm.find('#monitAdmin-AdminUI').html(window.i18n.msgStore['adminUI-MonitAdmin']);
  elm.find('#glancesAdmin-AdminUI').html(window.i18n.msgStore['adminUI-GlancesAdmin']);
  elm.find('#alertAdmin-AdminUI').html(window.i18n.msgStore['adminUI-AlertAdmin']);
  elm.find('#indexField-AdminUI').html(window.i18n.msgStore['adminUI-IndexField']);
  elm.find('#schemaAnalysis-AdminUI').html(window.i18n.msgStore['adminUI-SchemaAnalysis']);
  elm.find('#schemaAdmin-AdminUI').html(window.i18n.msgStore['adminUI-SchemaAdmin']);
  elm.find('#sizeLimitation-AdminUI').html(window.i18n.msgStore['adminUI-SizeLimitation']);
  elm.find('#autocompleteConfig-AdminUI').html(window.i18n.msgStore['adminUI-AutocompleteConfig']);
  elm.find('#searchEngineConfig-AdminUI').html(window.i18n.msgStore['adminUI-SearchEngineConfig']);

  elm.find('#searchAggregatorConf-AdminUI').html(window.i18n.msgStore['adminUI-SearchAggregatorConf']);
  elm.find('#departmentSearchConf-AdminUI').html(window.i18n.msgStore['adminUI-DepartmentSearchConf']);
  elm.find('#queryElevator-AdminUI').html(window.i18n.msgStore['adminUI-QueryElevator']);
  elm.find('#promoLinks-AdminUI').html(window.i18n.msgStore['adminUI-PromoLinks']);
  elm.find('#synonyms-AdminUI').html(window.i18n.msgStore['adminUI-Synonyms']);
  elm.find('#stopwords-AdminUI').html(window.i18n.msgStore['adminUI-Stopwords']);
  elm.find('#protwords-AdminUI').html(window.i18n.msgStore['adminUI-Protwords']);
  elm.find('#fieldWeight-AdminUI').html(window.i18n.msgStore['adminUI-FieldWeight']);
  elm.find('#fieldWeightAPI-AdminUI').html(window.i18n.msgStore['adminUI-FieldWeight']);
  elm.find('#facetConfig-AdminUI').html(window.i18n.msgStore['adminUI-FacetConfig']);
  elm.find('#likesFavoritesSearchEng-AdminUI').html(window.i18n.msgStore['adminUI-LikesAndFavorites']);
  elm.find('#servers-AdminUI').html(window.i18n.msgStore['adminUI-Servers']);
  
  // Users
  elm.find('#userManagement-AdminUI').html(window.i18n.msgStore['adminUI-UserManagement']);
  elm.find('#modifyUsers-AdminUI').html(window.i18n.msgStore['adminUI-ModifyUsers']);
  elm.find('#modifyServiceUsers-AdminUI').html(window.i18n.msgStore['adminUI-ModifyServiceUsers']);
  elm.find('#modifyDepartment-AdminUI').html(window.i18n.msgStore['adminUI-UserDepartment']);
  elm.find('#manageImportedUsers-AdminUI').html(window.i18n.msgStore['manageImportedUsers-AdminUI']);
  elm.find('#addUser-AdminUI').html(window.i18n.msgStore['adminUI-AddUser']);
  elm.find('#forgetUser-AdminUI').html(window.i18n.msgStore['adminUI-ForgetUser']);
  
  // Active Directory
  elm.find('#activeDirectoryManagement-AdminUI').html(window.i18n.msgStore['adminUI-activeDirectoryManagement']);
  elm.find('#ADConfig-AdminUI').html(window.i18n.msgStore['adminUI-ADConfig']);
  elm.find('#testADAuthority-AdminUI').html(window.i18n.msgStore['adminUI-testADAuthority']);
  
  // Expert menu
  elm.find('#expertMenu-AdminUI').html(window.i18n.msgStore['adminUI-expertMenu']);
  elm.find('#MCFBackupRestore-AdminUI').html(window.i18n.msgStore['adminUI-Connectors-BackupRestore']);
  $("#tikaCreator-AdminUI").html(window.i18n.msgStore['adminUI-tikaCreator']);
  //Extra Functionalities
  elm.find('#extraFunctionalities-AdminUI').html(window.i18n.msgStore['adminUI-extraFunctionalities']);
  elm.find('#deduplication-AdminUI').html(window.i18n.msgStore['adminUI-Deduplication']);
  $("#universalConnectorUploadConf-AdminUI").html(window.i18n.msgStore['universalConnectorUploadConf-AdminUI']);

  elm.find('#entityExtractionConf-AdminUI').html(window.i18n.msgStore['adminUI-entityExtractionConf']);
  elm.find('#sttEntitiesConfiguration-AdminUI').html(window.i18n.msgStore['adminUI-sttEntitiesConfiguration']);
  elm.find('#tagCloudConfiguration-AdminUI').html(window.i18n.msgStore['adminUI-tagCloudConf']);
  elm.find('#zookeeper-AdminUI').html(window.i18n.msgStore['adminUI-Zookeeper']);
  elm.find('#helpPage-AdminUI').html(window.i18n.msgStore['adminUI-helpPageEditor']);
  elm.find('#upload-AdminUI').html(window.i18n.msgStore['adminUI-UploadFile']);

  $("#logs-AdminUI").html(window.i18n.msgStore['adminUI-Logs']);
  $("#downloadLogs-AdminUI").html(window.i18n.msgStore['adminUI-Download-Logs']);

  $("#licence-AdminUI").html(window.i18n.msgStore['adminUI-Licence']);
});
