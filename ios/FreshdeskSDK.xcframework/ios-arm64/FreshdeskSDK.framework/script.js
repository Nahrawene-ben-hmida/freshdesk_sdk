//
//  script.swift
//  FreshdeskSDK
//
//  Created by Srivikashini Venkatachalam on 10/12/24.
//

const LOG = {
    V: 1,
    D: 2,
    I: 3,
    W: 4,
    E: 5
};

let log = {
    _log: function(priority, message) {
        let logInfo = { priority: priority, message: message }
        webkit.messageHandlers.onConsoleMessage.postMessage(logInfo);
    },
    v: (message) => log._log(LOG.V, message),
    d: (message) => log._log(LOG.D, message),
    i: (message) => log._log(LOG.I, message),
    w: (message) => log._log(LOG.W, message),
    e: (message) => log._log(LOG.E, message),
};


function initialSetupAndRegisterEvent() {
    setupWidgetEvents();
    setupUserEvents();
    setupMessageEvents();
    setupFileEvents();
}


function setupWidgetEvents() {
    log.d("Setup widget events invoked");

  window.fdWidget.on("widget:loaded", function(resp) {
      console.log('Widget loaded');
      listenUnreadCountChanged()
      webkit.messageHandlers.widgetLoaded.postMessage(resp);
  });
  
  window.fdWidget.on("widget:closed", function(resp) {
    console.log('Widget Closed');
    webkit.messageHandlers.widgetClosed.postMessage(resp);
  });
  
  window.fdWidget.on("widget:opened", function(resp) {
    console.log('Widget Opened');
    webkit.messageHandlers.widgetOpened.postMessage(resp);
  });
  
  window.fdWidget.on("widget:destroyed", function() {
    console.log('Widget Destroyed');
    webkit.messageHandlers.widgetDestroyed.postMessage();
  });
}

function setupUserEvents() {
  window.fdWidget.on("user:created", function(resp) {
    window.fdWidget.user.get().then(function(result) {
      log.d("User created");
      webkit.messageHandlers.onUserCreated.postMessage(result);
    }, function(error) {
      if(error.status = 401) {
        log.w("User does not exist")
      } else {
        log.e("Error getting user", JSON.stringify(error));
      }
    });
  });
  
  window.fdWidget.on("frame:statechange", function(data) {
      log.d("Frame state change");
      log.d(data.data.frameState);
      webkit.messageHandlers.onFrameStateChanged.postMessage(data.data.frameState);
  });
  
  window.fdWidget.on("user:statechange", function(data) {
    log.d("User state change");
    log.d(data.data.userState);
    webkit.messageHandlers.onUserStateChanged.postMessage(data.data.userState);
  });
  
  window.fdWidget.on("user:cleared", function(data) {
    console.log('User Cleared');
    webkit.messageHandlers.onUserCleared.postMessage(data);
  });
}

function setupMessageEvents() {
  window.fdWidget.on("message:sent", function(data) {
    console.log('Message Sent');
    webkit.messageHandlers.messageSent.postMessage(data);
  });
  
  window.fdWidget.on("message:received", function(data) {
    console.log('Message Received');
    webkit.messageHandlers.messageReceived.postMessage(data);
  });
}

function setupFileEvents() {
  window.fdWidget.on("download:file", function(response) {
    console.log('Download File');
    webkit.messageHandlers.downloadFile.postMessage(response);
  });
}

function listenUnreadCountChanged() {
    window.fdWidget.on("unreadCount:notify", function(resp) {
        log.d("Unread count changed");
        webkit.messageHandlers.unreadCountChanged.postMessage(resp);
    });
}

function getUserDetails(key) {
    let message = {
        key: key,
    }
    
    window.fdWidget.user.get().then(function(result) {
        message.value = result
        webkit.messageHandlers.onUserFetchSuccess.postMessage(message);
    }).catch(function(error) {
        message.value = error
        webkit.messageHandlers.onUserFetchFailure.postMessage(message);
    });
}

function getSessionToken(key) {
    let message = {
        key: key,
    }
    
    window.fdWidget.getToken().then(function(result) {
        message.value = result
        webkit.messageHandlers.onSessionTokenReceived.postMessage(message);
    }).catch(function(error) {
        message.value = error
        webkit.messageHandlers.onSessionTokenReceived.postMessage(message);
    });
}
