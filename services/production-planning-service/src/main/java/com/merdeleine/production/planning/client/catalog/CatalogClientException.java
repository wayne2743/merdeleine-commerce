package com.merdeleine.production.planning.client.catalog;

public class CatalogClientException extends RuntimeException {
    public CatalogClientException(String message) { super(message); }
    public CatalogClientException(String message, Throwable cause) { super(message, cause); }
}

class CatalogClientNotFoundException extends CatalogClientException {
    public CatalogClientNotFoundException(String message) { super(message); }
}

class CatalogClientConflictException extends CatalogClientException {
    public CatalogClientConflictException(String message) { super(message); }
}

class CatalogClientBadRequestException extends CatalogClientException {
    public CatalogClientBadRequestException(String message) { super(message); }
}

class CatalogClientServerException extends CatalogClientException {
    public CatalogClientServerException(String message) { super(message); }
}