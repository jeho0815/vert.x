/*
 * Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 * -------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.net;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.net.impl.SelfSignedCertificateImpl;

import java.security.cert.CertificateException;

/**
 * A self-signed certificate helper for testing and development purposes.
 * <p>
 * This helper is based on the {@link io.netty.handler.ssl.util.SelfSignedCertificate} Netty helper class.
 * While it helps for testing and development, it should never ever be used in production settings.
 *
 * @author <a href="https://julien.ponge.org/">Julien Ponge</a>
 * @see io.netty.handler.ssl.util.SelfSignedCertificate
 */
@VertxGen
public interface SelfSignedCertificate {

  /**
   * {@link KeyCertOptions} provider.
   *
   * @return a {@link PemKeyCertOptions} based on the generated certificate.
   */
  PemKeyCertOptions keyCertOptions();

  /**
   * {@link TrustOptions} provider.
   *
   * @return a {@link PemTrustOptions} based on the generated certificate.
   */
  PemTrustOptions trustOptions();

  /**
   * Filesystem path to the private key.
   *
   * @return the absolute path to the private key.
   */
  String privateKeyPath();

  /**
   * Filesystem path to the certificate.
   *
   * @return the absolute path to the certificate.
   */
  String certificatePath();

  /**
   * Delete the private key and certificate files.
   */
  void delete();

  /**
   * Create a new {@code SelfSignedCertificate} instance.
   *
   * @return a new instance.
   * @throws CertificateException in case a certificate could not be generated.
   */
  static SelfSignedCertificate create() throws CertificateException {
    return new SelfSignedCertificateImpl();
  }
}
