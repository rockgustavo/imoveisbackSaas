package br.com.rockgustavo.imobiliaria;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ContadorDeQueriesDataSource implements DataSource {

    private final DataSource delegate;
    private final AtomicInteger contador = new AtomicInteger();

    ContadorDeQueriesDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    public int contagemAtual() {
        return contador.get();
    }

    public void zerar() {
        contador.set(0);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return contando(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return contando(delegate.getConnection(username, password));
    }

    private Connection contando(Connection real) {
        InvocationHandler handler = (proxyObj, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                contador.incrementAndGet();
            }
            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class}, handler);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
