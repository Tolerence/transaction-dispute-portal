import { Navigate, Route, Routes } from 'react-router-dom';
import { isReviewer, isSignedIn } from './auth/session';
import { LoginPage } from './auth/LoginPage';
import { CallbackPage } from './auth/CallbackPage';
import { Layout } from './components/Layout';
import { DisputeDetailPage } from './pages/DisputeDetailPage';
import { DisputesPage } from './pages/DisputesPage';
import { TransactionDetailPage } from './pages/TransactionDetailPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { ReviewQueuePage } from './pages/ReviewQueuePage';
import { ReviewDisputePage } from './pages/ReviewDisputePage';

function RequireAuth({ children }: { children: JSX.Element }) {
  return isSignedIn() ? children : <Navigate to="/login" replace />;
}

function RequireReviewer({ children }: { children: JSX.Element }) {
  return isReviewer() ? children : <Navigate to="/transactions" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/callback" element={<CallbackPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/transactions" replace />} />
        <Route path="/transactions" element={<TransactionsPage />} />
        <Route path="/transactions/:id" element={<TransactionDetailPage />} />
        <Route path="/disputes" element={<DisputesPage />} />
        <Route path="/disputes/:id" element={<DisputeDetailPage />} />
        <Route
          path="/review"
          element={
            <RequireReviewer>
              <ReviewQueuePage />
            </RequireReviewer>
          }
        />
        <Route
          path="/review/:id"
          element={
            <RequireReviewer>
              <ReviewDisputePage />
            </RequireReviewer>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/transactions" replace />} />
    </Routes>
  );
}
