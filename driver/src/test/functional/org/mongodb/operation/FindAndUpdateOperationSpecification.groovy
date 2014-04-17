/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
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

package org.mongodb.operation
import category.Async
import org.junit.experimental.categories.Category
import org.mongodb.Document
import org.mongodb.FunctionalSpecification
import org.mongodb.codecs.DocumentCodec
import org.mongodb.test.CollectionHelper
import org.mongodb.test.Worker
import org.mongodb.test.WorkerCodec

import static org.mongodb.Fixture.getBinding
import static org.mongodb.Fixture.getSession

class FindAndUpdateOperationSpecification extends FunctionalSpecification {
    private final DocumentCodec documentCodec = new DocumentCodec()
    private final WorkerCodec workerCodec = new WorkerCodec()

    def 'should update single document'() {

        given:
        CollectionHelper<Document> helper = new CollectionHelper<Document>(documentCodec, getNamespace())
        Document pete = new Document('name', 'Pete').append('numberOfJobs', 3)
        Document sam = new Document('name', 'Sam').append('numberOfJobs', 5)

        helper.insertDocuments(pete, sam)

        when:
        def findAndUpdate = new FindAndUpdate()
                .where(new Document('name', 'Pete'))
                .updateWith(new Document('$inc', new Document('numberOfJobs', 1)))

        FindAndUpdateOperation<Document> operation = new FindAndUpdateOperation<Document>(getNamespace(), findAndUpdate,
                documentCodec)
        Document returnedDocument = operation.execute(getBinding())

        then:
        returnedDocument.getInteger('numberOfJobs') == 3
        helper.find().size() == 2;
        helper.find().get(0).getInteger('numberOfJobs') == 4
    }

    @Category(Async)
    def 'should update single document asynchronously'() {
        given:
        CollectionHelper<Document> helper = new CollectionHelper<Document>(documentCodec, getNamespace())
        Document pete = new Document('name', 'Pete').append('numberOfJobs', 3)
        Document sam = new Document('name', 'Sam').append('numberOfJobs', 5)

        helper.insertDocuments(pete, sam)

        when:
        FindAndUpdate findAndUpdate = new FindAndUpdate()
                .where(new Document('name', 'Pete'))
                .updateWith(new Document('$inc', new Document('numberOfJobs', 1)))

        FindAndUpdateOperation<Document> operation = new FindAndUpdateOperation<Document>(getNamespace(), findAndUpdate,
                documentCodec)
        Document returnedDocument = operation.executeAsync(getSession()).get()

        then:
        returnedDocument.getInteger('numberOfJobs') == 3
        helper.find().size() == 2;
        helper.find().get(0).getInteger('numberOfJobs') == 4
    }

    def 'should update single document when using custom codecs'() {
        given:
        CollectionHelper<Worker> helper = new CollectionHelper<Worker>(workerCodec, getNamespace())
        Worker pete = new Worker('Pete', 'handyman', new Date(), 3)
        Worker sam = new Worker('Sam', 'plumber', new Date(), 5)

        helper.insertDocuments(pete, sam)

        when:
        def findAndUpdate = new FindAndUpdate<Worker>()
                .where(new Document('name', 'Pete'))
                .updateWith(new Document('$inc', new Document('numberOfJobs', 1)))

        FindAndUpdateOperation<Worker> operation = new FindAndUpdateOperation<Worker>(getNamespace(), findAndUpdate,
                workerCodec)
        Worker returnedDocument = operation.execute(getBinding())

        then:
        returnedDocument.numberOfJobs == 3
        helper.find().size() == 2;
        helper.find().get(0).numberOfJobs == 4
    }

    @Category(Async)
    def 'should update single document when using custom codecs asynchronously'() {
        given:
        CollectionHelper<Worker> helper = new CollectionHelper<Worker>(workerCodec, getNamespace())
        Worker pete = new Worker('Pete', 'handyman', new Date(), 3)
        Worker sam = new Worker('Sam', 'plumber', new Date(), 5)

        helper.insertDocuments(pete, sam)

        when:
        FindAndUpdate<Worker> findAndUpdate = new FindAndUpdate<Worker>()
                .where(new Document('name', 'Pete'))
                .updateWith(new Document('$inc', new Document('numberOfJobs', 1)))

        FindAndUpdateOperation<Worker> operation = new FindAndUpdateOperation<Worker>(getNamespace(), findAndUpdate,
                workerCodec)
        Worker returnedDocument = operation.executeAsync(getSession()).get()

        then:
        returnedDocument.numberOfJobs == 3
        helper.find().size() == 2;
        helper.find().get(0).numberOfJobs == 4
    }
}