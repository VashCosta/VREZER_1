import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { GlassCard } from '../components/ui/GlassCard';
import { Button } from '../components/ui/Button';
import { Sparkles, Mail, Lock, User as UserIcon, Shield } from 'lucide-react';
import { UserRole } from '../types';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('alex.johnson@careerforge.io');
  const [password, setPassword] = useState('password123');
  const [role, setRole] = useState<UserRole>('STUDENT');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login('mock-jwt-token-12345', {
      id: 1,
      name: email.split('@')[0].replace('.', ' '),
      email,
      role,
      emailVerified: true
    });
    if (role === 'RECRUITER') navigate('/recruiter');
    else if (role === 'ADMIN') navigate('/admin');
    else navigate('/dashboard');
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-[#090d16]">
      <GlassCard className="w-full max-w-md p-8 border-slate-800">
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white mx-auto mb-4 shadow-lg shadow-blue-500/30">
            <Sparkles className="w-6 h-6" />
          </div>
          <h2 className="text-2xl font-extrabold text-white">Welcome Back</h2>
          <p className="text-xs text-slate-400 mt-1">Sign in to your VREZER AI workspace</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Select User Role</label>
            <div className="grid grid-cols-3 gap-2">
              {(['STUDENT', 'RECRUITER', 'ADMIN'] as UserRole[]).map((r) => (
                <button
                  key={r}
                  type="button"
                  onClick={() => setRole(r)}
                  className={`py-2 text-xs font-bold rounded-xl border transition ${
                    role === r
                      ? 'bg-blue-600/20 text-blue-400 border-blue-500/50'
                      : 'bg-slate-900 text-slate-400 border-slate-800 hover:bg-slate-850'
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Email Address</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
                required
              />
            </div>
          </div>

          <Button type="submit" className="w-full py-2.5">
            Sign In to {role} Portal
          </Button>
        </form>
      </GlassCard>
    </div>
  );
};
