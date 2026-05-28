#pragma once

#include "../include/ConnectionHandler.h"
#include "event.h"

// TODO: implement the STOMP protocol
class ClientState;

class StompProtocol
{
private:

ClientState& state;

    void handleConnected(const std::string& frame);
    void handleMessage(const std::string& frame);
    void handleReceipt(const std::string& frame);
    void handleError(const std::string& frame);
    std::string extractHeader(const std::string& frame, const std::string& headerName);
    std::string extractBody(const std::string& frame);
    Event parseEventBody(const std::string& body);

public:

StompProtocol(ClientState& clientState);
    
    void process(const std::string& message);
    bool shouldTerminate();
};


