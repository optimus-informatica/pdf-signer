/*
 *     Copyright © 2020 Optimus Informatica <http://optimus.osasco.br>
 *
 *     This file is part of PDFSigner.
 *
 *     PDFSigner is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PDFSigner is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>
 *
 *     Este arquivo é parte do programa PDFSigner
 *
 *     PDFSigner é um software livre; você pode redistribuí-lo e/ou
 *     modificá-lo dentro dos termos da Licença Pública Geral GNU como
 *     publicada pela Free Software Foundation (FSF); na versão 3 da
 *     Licença, ou (a seu critério) qualquer versão posterior.
 *
 *     Este programa é distribuído na esperança de que possa ser útil,
 *     mas SEM NENHUMA GARANTIA; sem uma garantia implícita de ADEQUAÇÃO
 *     a qualquer MERCADO ou APLICAÇÃO EM PARTICULAR. Veja a
 *     Licença Pública Geral GNU para maiores detalhes.
 *
 *     Você deve ter recebido uma cópia da Licença Pública Geral GNU junto
 *     com este programa, Se não, veja <http://www.gnu.org/licenses/>.
 */

package br.osasco.optimus.pdfsigner

class AppConfigs {
    File configFile = new File(System.properties["user.home"] + "/config.groovy")

    def loadConfigs() {
        ConfigSlurper slurper = new ConfigSlurper()
        if (!configFile.exists()) {
            try {
                makeNewFile()
            }
            catch (IOException ex) {
                println("Erro ao tentar criar o arquivo de configuração ${ex.message}")
            }
        }
        slurper.parse(configFile.toURL())
    }

    private void makeNewFile() {
        InputStream is = getClass().getResourceAsStream("/config.groovy")
        byte[] buffer = new byte[is.available()]
        is.read(buffer)
        OutputStream os = new FileOutputStream(configFile, false)
        os.write(buffer)
        os.close()
        is.close()
    }

}
