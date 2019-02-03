package com.gameball.gameball.network.utils;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/14/2017.
 */
public class RetrofitException extends RuntimeException {
    private final String url;
    private final Response response;
    private final Kind kind;
    private final Retrofit retrofit;

    /**
     * Instantiates a new Retrofit exception.
     *
     * @param message   the message
     * @param url       the url
     * @param response  the response
     * @param kind      the kind
     * @param exception the exception
     * @param retrofit  the retrofit
     */
    RetrofitException(String message, String url, Response response, Kind kind, Throwable exception, Retrofit retrofit) {
        super(message, exception);
        this.url = url;
        this.response = response;
        this.kind = kind;
        this.retrofit = retrofit;
    }

    /**
     * Http error retrofit exception.
     *
     * @param url      the url
     * @param response the response
     * @param retrofit the retrofit
     * @return the retrofit exception
     */
    public static RetrofitException httpError(String url, Response response, Retrofit retrofit) {
        String message = response.code() + " " + response.message();
        return new RetrofitException(message, url, response, Kind.HTTP, null, retrofit);
    }

    /**
     * Network error retrofit exception.
     *
     * @param exception the exception
     * @return the retrofit exception
     */
    public static RetrofitException networkError(IOException exception) {
        return new RetrofitException(exception.getMessage(), null, null, Kind.NETWORK, exception, null);
    }

    /**
     * Unexpected error retrofit exception.
     *
     * @param exception the exception
     * @return the retrofit exception
     */
    public static RetrofitException unexpectedError(Throwable exception) {
        return new RetrofitException(exception.getMessage(), null, null, Kind.UNEXPECTED, exception, null);
    }

    /**
     * The request URL which produced the error.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Response object containing status code, headers, body, etc.
     *
     * @return the response
     */
    public Response getResponse() {
        return response;
    }

    /**
     * The event kind which triggered this error.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * The Retrofit this request was executed on
     *
     * @return the retrofit
     */
    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * HTTP response body converted to specified {@code type}. {@code null} if there is no
     * response.
     *
     * @param <T>  the type parameter
     * @param type the type
     * @return the error body as
     * @throws IOException if unable to convert the body to the specified {@code type}.
     */
    public <T> T getErrorBodyAs(Class<T> type) throws IOException {
        if (response == null || response.errorBody() == null) {
            return null;
        }
        Converter<ResponseBody, T> converter = retrofit.responseBodyConverter(type, new Annotation[0]);
        return converter.convert(response.errorBody());
    }

    /**
     * Identifies the event kind which triggered a {@link RetrofitException}.
     */
    public enum Kind {
        /**
         * An {@link IOException} occurred while communicating to the server.
         */
        NETWORK,
        /**
         * A non-200 HTTP status code was received from the server.
         */
        HTTP,
        /**
         * An internal error occurred while attempting to execute a request. It is best practice to
         * re-throw this exception so your application crashes.
         */
        UNEXPECTED
    }
}