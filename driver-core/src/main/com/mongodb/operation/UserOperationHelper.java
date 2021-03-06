/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.operation;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoCredential;
import com.mongodb.MongoInternalException;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.lang.NonNull;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.Arrays;

import static com.mongodb.internal.authentication.NativeAuthenticationHelper.createAuthenticationHash;
import static com.mongodb.operation.WriteConcernHelper.createWriteConcernError;
import static com.mongodb.operation.WriteConcernHelper.hasWriteConcernError;

final class UserOperationHelper {

    static BsonDocument asCommandDocument(final MongoCredential credential, final boolean readOnly, final String commandName) {
        BsonDocument document = new BsonDocument();
        document.put(commandName, new BsonString(credential.getUserName()));
        document.put("pwd", new BsonString(createAuthenticationHash(getUserNameNonNull(credential), getPasswordNonNull(credential))));
        document.put("digestPassword", BsonBoolean.FALSE);
        document.put("roles", new BsonArray(Arrays.<BsonValue>asList(new BsonString(getRoleName(credential, readOnly)))));
        return document;
    }

    private static String getRoleName(final MongoCredential credential, final boolean readOnly) {
        return credential.getSource().equals("admin")
               ? (readOnly ? "readAnyDatabase" : "root") : (readOnly ? "read" : "dbOwner");
    }

    static void translateUserCommandException(final MongoCommandException e) {
        if (e.getErrorCode() == 100 && hasWriteConcernError(e.getResponse())) {
            throw createWriteConcernError(e.getResponse(), e.getServerAddress());
        } else {
            throw e;
        }
    }

    static SingleResultCallback<Void> userCommandCallback(final SingleResultCallback<Void> wrappedCallback) {
        return new SingleResultCallback<Void>() {
            @Override
            public void onResult(final Void result, final Throwable t) {
                if (t != null) {
                    if (t instanceof MongoCommandException
                                && hasWriteConcernError(((MongoCommandException) t).getResponse())) {
                        wrappedCallback.onResult(null,
                                createWriteConcernError(((MongoCommandException) t).getResponse(),
                                        ((MongoCommandException) t).getServerAddress()));
                    } else {
                        wrappedCallback.onResult(null, t);
                    }
                } else {
                    wrappedCallback.onResult(null, null);
                }
            }
        };
    }

    @NonNull
    private static String getUserNameNonNull(final MongoCredential credential) {
        String userName = credential.getUserName();
        if (userName == null) {
            throw new MongoInternalException("User name can not be null");
        }
        return userName;
    }

    @NonNull
    private static char[] getPasswordNonNull(final MongoCredential credential) {
        char[] password = credential.getPassword();
        if (password == null) {
            throw new MongoInternalException("Password can not be null");
        }
        return password;
    }

    private UserOperationHelper() {
    }
}
