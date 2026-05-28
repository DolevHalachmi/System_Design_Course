#pragma once

#include <string>
#include <map>
#include <vector>
#include <mutex>
#include <atomic>
#include "../include/ConnectionHandler.h"
#include "../include/event.h" 

class ClientState {
private:
    std::string username;
    std::atomic<bool> isConnected;
    std::atomic<int> subscriptionIdCounter;
    std::atomic<int> receiptIdCounter;
    std::map<std::string, int> channelToSubId;
    std::map<std::string, std::map<std::string, std::vector<Event>>> gameHistory;
    std::mutex stateMutex;
    
    ConnectionHandler* handler;

public:
    ClientState();

    ClientState(const ClientState&) = delete;
    ClientState& operator=(const ClientState&) = delete;
    virtual ~ClientState();

    void setConnectionHandler(ConnectionHandler* ch);
    ConnectionHandler* getHandler();
    bool isLoggedIn();
    void setLoggedIn(bool status);
    void setUsername(std::string user);
    std::string getUsername();

    int generateSubscriptionId();
    int generateReceiptId();

    void addSubscription(const std::string& channelName, int subId);
    void removeSubscription(const std::string& channelName);
    void removeSubscriptionById(int subId); 
    int getSubscriptionId(const std::string& channelName);
    bool isSubscribed(const std::string& channelName);

    void addEvent(const std::string& channelName, const std::string& username, const Event& event);
    std::string getSummary(const std::string& channelName, const std::string& username);
    void clearState(); 
};